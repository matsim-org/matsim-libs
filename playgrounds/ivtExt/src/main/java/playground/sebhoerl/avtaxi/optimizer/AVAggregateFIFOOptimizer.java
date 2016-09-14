package playground.sebhoerl.avtaxi.optimizer;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.contrib.taxi.schedule.TaxiEmptyDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiSchedules;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.taxi.scheduler.TaxiSchedulerParams;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;

import playground.sebhoerl.avtaxi.optimizer.aggregation.AVAggregateTaxiRequest;
import playground.sebhoerl.avtaxi.optimizer.aggregation.AVTaxiRequestAggregator;
import playground.sebhoerl.avtaxi.schedule.AVTaxiMultiDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVTaxiMultiOccupiedDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVTaxiMultiPickupTask;

public class AVAggregateFIFOOptimizer implements TaxiOptimizer {
	final private TaxiOptimizerContext context;
	final private LeastCostPathCalculator router;
	final private TaxiSchedulerParams params;
	
	final private Queue<Vehicle> vehicles = new LinkedList<>();

	private boolean reoptimize = true;
		
	final static double MAXIMUM_AGGREGATION_DELAY = 60;
	
	final private Queue<TaxiRequest> unplannedMasterRequests = new LinkedList<>();
	final private Map<TaxiRequest, Set<TaxiRequest>> slaveRequests = new HashMap<>();
	
	public AVAggregateFIFOOptimizer(TaxiOptimizerContext context, TaxiSchedulerParams params) {
		this.context = context;
		this.params = params;
		
		router = new Dijkstra(context.network, context.travelDisutility, context.travelTime);
		vehicles.addAll(context.taxiData.getVehicles().values());
	}
	
	boolean requestsAreCombinable(TaxiRequest master, TaxiRequest slave) {
		if (!master.getFromLink().equals(slave.getFromLink())) return false;
		if (!master.getToLink().equals(slave.getToLink())) return false;
		return Math.abs(master.getT0() - slave.getT0()) < MAXIMUM_AGGREGATION_DELAY;
	}
	
	@Override
	public void requestSubmitted(Request req) {
		TaxiRequest request = (TaxiRequest) req;
		TaxiRequest master = null;
		
		for (TaxiRequest m : unplannedMasterRequests) {
			if (requestsAreCombinable(m, request)) {
				master = m;
				break;
			}
		}
		
		if (master == null) {
			System.err.println(String.format("MASTER: %s -> %s @ %d (%s)", request.getFromLink().getId().toString(), request.getToLink().getId().toString(), (int)request.getT0(), request.getPassenger().getId().toString()));
			unplannedMasterRequests.add(request);
		} else {
			System.err.println(String.format("SLAVE: %s -> %s @ %d (%s > %s)", master.getFromLink().getId().toString(), master.getToLink().getId().toString(), (int)master.getT0(), master.getPassenger().getId().toString(), request.getPassenger().getId().toString()));
			
			Set<TaxiRequest> slaves = slaveRequests.get(master);
			
			if (slaves == null) {
				slaves = new HashSet<>();
				slaveRequests.put(master, slaves);
			}
			
			slaves.add(request);
		}
		
		reoptimize = true;
	}
	
	@Override
	public void nextLinkEntered(DriveTask driveTask) {}
	
	private void optimize() {
		reoptimize = false;
		
		while (!vehicles.isEmpty() && !unplannedMasterRequests.isEmpty()) {
			TaxiRequest master = unplannedMasterRequests.poll();
			Set<TaxiRequest> slaves = slaveRequests.remove(master);
			Vehicle vehicle = vehicles.poll();
			
			optimizeAssignment(vehicle, master, (slaves == null) ? Collections.emptySet() : slaves);
		}
	}
	
	private void optimizeAssignment(Vehicle vehicle, TaxiRequest master, Set<TaxiRequest> slaves) {
		Schedule<TaxiTask> schedule = TaxiSchedules.asTaxiSchedule(vehicle.getSchedule());
		
		TaxiStayTask stayTask = (TaxiStayTask) Schedules.getLastTask(schedule);
		
		double startTime = 0.0;
		double scheduleEndTime = schedule.getEndTime();
		
		if (stayTask.getStatus() == TaskStatus.STARTED) {
			startTime = context.timer.getTimeOfDay();
		} else {
			startTime = stayTask.getBeginTime();
		}
		
		VrpPathWithTravelData pickupPath = VrpPaths.calcAndCreatePath(stayTask.getLink(), master.getFromLink(), startTime, router, context.travelTime);
		VrpPathWithTravelData dropoffPath = VrpPaths.calcAndCreatePath(master.getFromLink(), master.getToLink(), pickupPath.getArrivalTime() + params.pickupDuration, router, context.travelTime);
		
		TaxiEmptyDriveTask emptyDriveTask = new TaxiEmptyDriveTask(pickupPath);
		AVTaxiMultiPickupTask pickupTask = new AVTaxiMultiPickupTask(pickupPath.getArrivalTime(), pickupPath.getArrivalTime() + params.pickupDuration);
		AVTaxiMultiOccupiedDriveTask occupiedDriveTask = new AVTaxiMultiOccupiedDriveTask(dropoffPath);
		AVTaxiMultiDropoffTask dropoffTask = new AVTaxiMultiDropoffTask(dropoffPath.getArrivalTime(), dropoffPath.getArrivalTime() + params.dropoffDuration);

		pickupTask.addRequest(master);
		occupiedDriveTask.addRequest(master);
		dropoffTask.addRequest(master);
		
		for (TaxiRequest slave : slaves) {
			pickupTask.addRequest(slave);
			occupiedDriveTask.addRequest(slave);
			dropoffTask.addRequest(slave);
		}
		
		if (stayTask.getStatus() == TaskStatus.STARTED) {
			stayTask.setEndTime(startTime);
		} else {
			schedule.removeLastTask();
		}
		
		schedule.addTask(emptyDriveTask);
		schedule.addTask(pickupTask);
		schedule.addTask(occupiedDriveTask);
		schedule.addTask(dropoffTask);
		
		schedule.addTask(new TaxiStayTask(dropoffTask.getEndTime(), scheduleEndTime, dropoffTask.getLink()));
	}
	
	@Override
	public void nextTask(Schedule<? extends Task> schedule) {
		if (schedule.getStatus() != ScheduleStatus.STARTED) {
			schedule.nextTask();
			return;
		}
		
		double now = context.timer.getTimeOfDay();
		
		Task currentTask = schedule.getCurrentTask();
		currentTask.setEndTime(now);
		
		List<TaxiTask> tasks = TaxiSchedules.asTaxiSchedule(schedule).getTasks();
		int index = currentTask.getTaskIdx() + 1;
		TaxiTask indexTask;
		
		TaxiTask nextTask = null;
		
		if (index + 1 < tasks.size()) {
			nextTask = tasks.get(index + 1);
		}
		
		double startTime = now;
		
		while (index < tasks.size()) {
			indexTask = (TaxiTask) tasks.get(index);
			
			if (indexTask.getTaxiTaskType() == TaxiTaskType.STAY) {
				if (indexTask.getEndTime() < startTime) indexTask.setEndTime(startTime);
			} else {
				indexTask.setEndTime(indexTask.getEndTime() - indexTask.getBeginTime() + startTime);
			}
			
			indexTask.setBeginTime(startTime);
			startTime = indexTask.getEndTime();
			index++;
		}
		
		schedule.nextTask();
		
		if (nextTask != null) {
			if (nextTask.getTaxiTaskType() == TaxiTaskType.STAY) {
				vehicles.add(schedule.getVehicle());
				reoptimize = true;
			}
		}
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (reoptimize) optimize();
	}
}
