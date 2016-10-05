package playground.sebhoerl.avtaxi.refactor;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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

import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;

public class AVSharedTaxiTwoRequestsOptimizer implements TaxiOptimizer {
	final private TaxiOptimizerContext context;
	final private LeastCostPathCalculator router;
	final private TaxiSchedulerParams params;
	
	final private Vehicle vehicle;
	
	final private Queue<TaxiRequest> requests = new LinkedList<>();
	
	public AVSharedTaxiTwoRequestsOptimizer(TaxiOptimizerContext context, TaxiSchedulerParams params) {
		this.context = context;
		this.params = params;
		
		router = new Dijkstra(context.network, context.travelDisutility, context.travelTime);
		vehicle = context.taxiData.getVehicles().values().iterator().next();
	}
	
	@Override
	public void nextLinkEntered(DriveTask driveTask) {}

	void optimizeTwoRequests() {
		TaxiRequest req1 = requests.poll();
		TaxiRequest req2 = requests.poll();
		Schedule<TaxiTask> schedule = TaxiSchedules.asTaxiSchedule(vehicle.getSchedule());
		
		TaxiStayTask stayTask = (TaxiStayTask) Schedules.getLastTask(schedule);
		
		double startTime = 0.0;
		double scheduleEndTime = schedule.getEndTime();
		
		if (stayTask.getStatus() == TaskStatus.STARTED) {
			startTime = context.timer.getTimeOfDay();
		} else {
			startTime = stayTask.getBeginTime();
		}
		
		VrpPathWithTravelData pickupPath = VrpPaths.calcAndCreatePath(stayTask.getLink(), req1.getFromLink(), startTime, router, context.travelTime);
		VrpPathWithTravelData dropoffPath = VrpPaths.calcAndCreatePath(req1.getFromLink(), req1.getToLink(), pickupPath.getArrivalTime() + params.pickupDuration, router, context.travelTime);
		
		TaxiEmptyDriveTask emptyDriveTask = new TaxiEmptyDriveTask(pickupPath);
		//TaxiPickupTask pickupTask = new TaxiPickupTask(pickupPath.getArrivalTime(), pickupPath.getArrivalTime() + params.pickupDuration, req);
		
		AVPickupTask pickupTask = new AVPickupTask(pickupPath.getArrivalTime(), pickupPath.getArrivalTime() + params.pickupDuration);
		pickupTask.addRequest(req1);
		pickupTask.addRequest(req2);
		
		//TaxiOccupiedDriveTask occupiedDriveTask = new TaxiOccupiedDriveTask(dropoffPath, req1);
		
		AVDriveTask occupiedDriveTask = new AVDriveTask(dropoffPath);
		occupiedDriveTask.addRequest(req1);
		occupiedDriveTask.addRequest(req2);
		
		//TaxiDropoffTask dropoffTask = new TaxiDropoffTask(dropoffPath.getArrivalTime(), dropoffPath.getArrivalTime() + params.dropoffDuration, req);
		
		AVDropoffTask dropoffTask = new AVDropoffTask(dropoffPath.getArrivalTime(), dropoffPath.getArrivalTime() + params.dropoffDuration);
		dropoffTask.addRequest(req1);
		dropoffTask.addRequest(req2);
		
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
	public void requestSubmitted(Request request) {
		requests.add((TaxiRequest) request);
		
		if (requests.size() == 2) {
			optimizeTwoRequests();
		} else if (requests.size() > 2) {
			throw new IllegalStateException();
		}
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
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {

	}
}
