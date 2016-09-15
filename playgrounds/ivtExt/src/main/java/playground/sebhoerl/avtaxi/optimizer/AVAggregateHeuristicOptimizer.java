package playground.sebhoerl.avtaxi.optimizer;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
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
import org.matsim.contrib.taxi.schedule.TaxiEmptyDriveTask;
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
import playground.sebhoerl.avtaxi.schedule.AVTaxiMultiDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVTaxiMultiOccupiedDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVTaxiMultiPickupTask;

public class AVAggregateHeuristicOptimizer implements TaxiOptimizer {
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
	
	final private Map<TaxiRequest, Set<TaxiRequest>> slaveRequests = new HashMap<>();
    
	// Only AVs with seats left are kept in those maps
    final private Map<Link, Set<TaxiRequest>> availableMasterRequestsByOrigin = new HashMap<>();
    final private Map<Link, Set<TaxiRequest>> availableMasterRequestsByDestination = new HashMap<>();
	
	class EnrouteMapping {
		AVTaxiMultiPickupTask pickup;
		AVTaxiMultiDropoffTask dropoff;
		
		public EnrouteMapping(AVTaxiMultiPickupTask pickup, AVTaxiMultiDropoffTask dropoff) {
			this.pickup = pickup;
			this.dropoff = dropoff;
		}
	}
	
	final private Map<TaxiRequest, EnrouteMapping> enrouteMappings = new HashMap<>();
	
	public AVAggregateHeuristicOptimizer(TaxiOptimizerContext context, TaxiSchedulerParams params, AVAggregateHeuristicOptimizerParams optimParams) {
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
		
		for (Link link : context.network.getLinks().values()) {
			availableMasterRequestsByOrigin.put(link, new HashSet<>());
			availableMasterRequestsByDestination.put(link, new HashSet<>());
		}
	}
	
	boolean seatsAreAvailable(TaxiRequest master) {
		Set<TaxiRequest> slaves = slaveRequests.get(master);
		if (slaves == null) return true;
		return slaves.size() < optimParams.maximumPassengers - 1;
	}
	
	private TaxiRequest findCombinableMasterRequest(TaxiRequest request) {
		Set<TaxiRequest> sameOrigin = availableMasterRequestsByOrigin.get(request.getFromLink());
		Set<TaxiRequest> sameDestination = availableMasterRequestsByDestination.get(request.getToLink());
		
		if (sameOrigin.size() > 0 && sameDestination.size() > 0) {
			Set<TaxiRequest> sameOD = new HashSet<>(sameOrigin);
			sameOD.retainAll(sameDestination);
			
			for (TaxiRequest candidate : sameOD) {
				if (Math.abs(candidate.getT0() - request.getT0()) < optimParams.maximumAggregationDelay) {
					return candidate;
				}
			}
		}
		
		return null;
	}
	
	@Override
	public void requestSubmitted(Request req) {
		TaxiRequest request = (TaxiRequest) req;
		TaxiRequest master = findCombinableMasterRequest(request);
		
		if (master == null) {
			//System.err.println(String.format("MASTER: %s -> %s @ %d (%s)", request.getFromLink().getId().toString(), request.getToLink().getId().toString(), (int)request.getT0(), request.getPassenger().getId().toString()));
			
			Coord coord = request.getFromLink().getCoord();
			
			unplannedMasterRequests.add(request);
			unplannedMasterRequestGrid.update(request, coord.getX(), coord.getY());
			
			if (optimParams.maximumPassengers > 1) {
				availableMasterRequestsByOrigin.get(request.getFromLink()).add(request);
				availableMasterRequestsByDestination.get(request.getToLink()).add(request);
			}
			
			reoptimize = true;
		} else {
			Set<TaxiRequest> slaves = slaveRequests.get(master);
			
			if (slaves == null) {
				slaves = new HashSet<>();
				slaveRequests.put(master, slaves);
			}
			
			slaves.add(request);
			
			if (!unplannedMasterRequests.contains(master)) { // AV is already on the way to the pickup location
				//System.err.println(String.format("SLAVE: %s -> %s @ %d (%s > %s) [enroute]", master.getFromLink().getId().toString(), master.getToLink().getId().toString(), (int)master.getT0(), master.getPassenger().getId().toString(), request.getPassenger().getId().toString()));
				assignEnrouteRequest(master, request);
			} else {
				//System.err.println(String.format("SLAVE: %s -> %s @ %d (%s > %s)", master.getFromLink().getId().toString(), master.getToLink().getId().toString(), (int)master.getT0(), master.getPassenger().getId().toString(), request.getPassenger().getId().toString()));
			}
			
			if (!seatsAreAvailable(master)) {
				availableMasterRequestsByOrigin.get(master.getFromLink()).remove(master);
				availableMasterRequestsByDestination.get(master.getToLink()).remove(master);
			}
		}
	}
	
	private void assignEnrouteRequest(TaxiRequest master, TaxiRequest slave) {
		EnrouteMapping mapping = enrouteMappings.get(master);
		mapping.pickup.addRequest(slave);
		mapping.dropoff.addRequest(slave);
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
	
	private void optimizeAssignment(Vehicle vehicle, TaxiRequest master) {
		Schedule<TaxiTask> schedule = TaxiSchedules.asTaxiSchedule(vehicle.getSchedule());
		Set<TaxiRequest> slaves = slaveRequests.get(master);
		
		if (slaves == null) {
			slaves = Collections.emptySet();
		}
		
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
		
		if (dropoffTask.getEndTime() < scheduleEndTime) {
			schedule.addTask(new TaxiStayTask(dropoffTask.getEndTime(), scheduleEndTime, dropoffTask.getLink()));
		}		
		
		enrouteMappings.put(master, new EnrouteMapping(pickupTask, dropoffTask));
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
			} else if (nextTask.getTaxiTaskType() == TaxiTaskType.PICKUP && nextTask instanceof AVTaxiMultiPickupTask) {
				for (TaxiRequest r : ((AVTaxiMultiPickupTask)nextTask).getRequests()) {
					// TODO: Only necessary to remove MASTER request, but then a specific AVTaxiRequest needs to be defined
					availableMasterRequestsByOrigin.get(r.getFromLink()).remove(r);
					availableMasterRequestsByDestination.get(r.getToLink()).remove(r);
					enrouteMappings.remove(r);
					slaveRequests.remove(r);
				}
			}
		}
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (reoptimize) optimize();
	}
}
