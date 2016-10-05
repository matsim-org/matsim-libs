package playground.sebhoerl.avtaxi.refactor;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.contrib.taxi.schedule.TaxiDropoffTask;
import org.matsim.contrib.taxi.schedule.TaxiEmptyDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiOccupiedDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiPickupTask;
import org.matsim.contrib.taxi.schedule.TaxiSchedules;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.taxi.scheduler.TaxiSchedulerParams;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import playground.sebhoerl.av.utils.Grid;

public class AVHeuristicOptimizer implements TaxiOptimizer {
    enum Mode {
        OVERSUPPLY, UNDERSUPPLY
    }
    
    private Mode mode = Mode.OVERSUPPLY;
	
	final private TaxiOptimizerContext context;
	final private LeastCostPathCalculator router;
	final private TaxiSchedulerParams params;
	final private AVAggregateHeuristicOptimizerParams optimParams;
	
	private boolean reoptimize = true;
	
	final private Queue<Vehicle> availableVehicles = new LinkedList<>();
	final private Grid<Vehicle> availableVehicleGrid;
	
	final private Queue<TaxiRequest> unplannedMasterRequests = new LinkedList<>();
	final private Grid<TaxiRequest> unplannedMasterRequestGrid;
	
	public AVHeuristicOptimizer(TaxiOptimizerContext context, TaxiSchedulerParams params, AVAggregateHeuristicOptimizerParams optimParams) {
		this.context = context;
		this.params = params;
		this.optimParams = optimParams;
		
		double bounds[] = NetworkUtils.getBoundingBox(context.network.getNodes().values());
		availableVehicleGrid = new Grid<Vehicle>(bounds[0], bounds[1], bounds[2], bounds[3], optimParams.grid_x, optimParams.grid_y);
		unplannedMasterRequestGrid = new Grid<TaxiRequest>(bounds[0], bounds[1], bounds[2], bounds[3], optimParams.grid_x, optimParams.grid_y);
		
		//router = new Dijkstra(context.network, context.travelDisutility, context.travelTime);
		TravelTime tt = new FreeSpeedTravelTime();
		router = new Dijkstra(context.network, new OnlyTimeDependentTravelDisutility(tt), tt);
		
		for (Vehicle vehicle : context.taxiData.getVehicles().values()) {
			Coord coord = vehicle.getStartLink().getCoord();
			availableVehicleGrid.update(vehicle, coord.getX(), coord.getY());
			availableVehicles.add(vehicle);
		}
	}
	
	@Override
	public void requestSubmitted(Request req) {
		TaxiRequest request = (TaxiRequest) req;
		Coord coord = request.getFromLink().getCoord();
		
		unplannedMasterRequests.add(request);
		unplannedMasterRequestGrid.update(request, coord.getX(), coord.getY());

		reoptimize = true;
	}
	
	private TaxiRequest getClosestMasterRequest(Vehicle vehicle) {
		if (unplannedMasterRequests.size() > 0) {
			Coord coord = ((StayTask) vehicle.getSchedule().getCurrentTask()).getLink().getCoord();
			return unplannedMasterRequestGrid.getClosest(coord.getX(), coord.getY(), 1).iterator().next();
		} else {
			return null;
		}
	}
	
	private Vehicle getClosestVehicle(TaxiRequest master) {
		if (availableVehicles.size() > 0) {
			Coord coord = master.getFromLink().getCoord();
			return availableVehicleGrid.getClosest(coord.getX(), coord.getY(), 1).iterator().next();
		} else {
			return null;
		}
	}
	
	@Override
	public void nextLinkEntered(DriveTask driveTask) {}
	
	private void optimize() {
		reoptimize = false;
		
		while (!availableVehicles.isEmpty() && !unplannedMasterRequests.isEmpty()) {
			TaxiRequest request = null;
			Vehicle vehicle = null;
			 
			switch (mode) {
			case OVERSUPPLY:
				request = unplannedMasterRequests.poll();
				unplannedMasterRequestGrid.remove(request);
				
				vehicle = getClosestVehicle(request);
				availableVehicleGrid.remove(vehicle);
				availableVehicles.remove(vehicle);
				
				break;
			case UNDERSUPPLY:
				vehicle = availableVehicles.poll();
				availableVehicleGrid.remove(vehicle);
				
				request = getClosestMasterRequest(vehicle);
				unplannedMasterRequestGrid.remove(request);
				unplannedMasterRequests.remove(request);
				
				break;
			}
			
			if (request != null && vehicle != null) {
				optimizeAssignment(vehicle, request);
			} else {
				throw new IllegalStateException();
				//break;
			}
		}
		
		mode = availableVehicles.size() > 0 ? Mode.OVERSUPPLY : Mode.UNDERSUPPLY;
	}
	
	private void optimizeAssignment(Vehicle vehicle, TaxiRequest request) {
		Schedule<TaxiTask> schedule = TaxiSchedules.asTaxiSchedule(vehicle.getSchedule());
		TaxiStayTask stayTask = (TaxiStayTask) Schedules.getLastTask(schedule);
		
		double startTime = 0.0;
		double scheduleEndTime = schedule.getEndTime();
		
		if (stayTask.getStatus() == TaskStatus.STARTED) {
			startTime = context.timer.getTimeOfDay();
		} else {
			startTime = stayTask.getBeginTime();
		}
		
		VrpPathWithTravelData pickupPath = VrpPaths.calcAndCreatePath(stayTask.getLink(), request.getFromLink(), startTime, router, context.travelTime);
		VrpPathWithTravelData dropoffPath = VrpPaths.calcAndCreatePath(request.getFromLink(), request.getToLink(), pickupPath.getArrivalTime() + params.pickupDuration, router, context.travelTime);
		
		TaxiEmptyDriveTask emptyDriveTask = new TaxiEmptyDriveTask(pickupPath);
		TaxiPickupTask pickupTask = new TaxiPickupTask(pickupPath.getArrivalTime(), pickupPath.getArrivalTime() + params.pickupDuration, request);
		TaxiOccupiedDriveTask occupiedDriveTask = new TaxiOccupiedDriveTask(dropoffPath, request);
		TaxiDropoffTask dropoffTask = new TaxiDropoffTask(dropoffPath.getArrivalTime(), dropoffPath.getArrivalTime() + params.dropoffDuration, request);
		
		if (stayTask.getStatus() == TaskStatus.STARTED) {
			stayTask.setEndTime(startTime);
		} else {
			schedule.removeLastTask();
		}
		
		schedule.addTask(emptyDriveTask);
		schedule.addTask(pickupTask);
		schedule.addTask(occupiedDriveTask);
		schedule.addTask(dropoffTask);
		
		if (dropoffTask.getEndTime() < scheduleEndTime) {
			schedule.addTask(new TaxiStayTask(dropoffTask.getEndTime(), scheduleEndTime, dropoffTask.getLink()));
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
		
		TaxiTask nextTask = null;
		
		if (index < tasks.size()) {
			nextTask = tasks.get(index);
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
				Coord coord = ((TaxiStayTask) nextTask).getLink().getCoord();
				availableVehicles.add(schedule.getVehicle());
				availableVehicleGrid.update(schedule.getVehicle(), coord.getX(), coord.getY());
				reoptimize = true;
			}
		}
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (reoptimize) optimize();
	}
}
