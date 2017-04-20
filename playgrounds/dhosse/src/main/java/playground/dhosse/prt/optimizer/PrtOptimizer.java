package playground.dhosse.prt.optimizer;

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.schedule.*;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;

import playground.dhosse.prt.request.NPersonsVehicleRequestPathFinder;

public class PrtOptimizer implements VrpOptimizerWithOnlineTracking, MobsimBeforeSimStepListener {

	protected final Collection<TaxiRequest> unplannedRequests;
	
	protected boolean requiresReoptimization = false;
	
	private Set<Vehicle> idleVehicles;
	
	//private final VrpData data;
	private final TaxiScheduler scheduler;
	private TaxiOptimizerContext optimizerContext;
	
	private final NPersonsVehicleRequestPathFinder vrpFinder;
	
	public PrtOptimizer(TaxiOptimizerContext optimizerContext){
		
		this.optimizerContext = optimizerContext;
		this.scheduler = optimizerContext.scheduler;
		this.unplannedRequests = new ArrayList<TaxiRequest>();
		
		int vehicleCapacity = ((PrtOptimizerContext)optimizerContext).prtConfigGroup.getVehicleCapacity();
		vrpFinder =  new NPersonsVehicleRequestPathFinder(optimizerContext, vehicleCapacity);
		
	}
	
	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if(this.requiresReoptimization){
			scheduleUnplannedRequests();
			this.requiresReoptimization = false;
		}
	}
	
	@Override
	public void requestSubmitted(Request request) {
		
		this.unplannedRequests.add((TaxiRequest) request);
		this.requiresReoptimization = true;
		
	}

	@Override
	public void nextTask(Vehicle vehicle) {
        this.scheduler.updateBeforeNextTask(vehicle);
        Task newCurrentTask = vehicle.getSchedule().nextTask();

        if (newCurrentTask != null // schedule != COMPLETED
                && ((TaxiTask)newCurrentTask).getTaxiTaskType() == TaxiTask.TaxiTaskType.STAY) {
            requiresReoptimization = true;
        }
	}
	
	@Override
	public void vehicleEnteredNextLink(Vehicle vehicle, Link nextLink)
	{
		scheduler.updateTimeline(vehicle);
	}
	
	protected void scheduleUnplannedRequests()
    {
        initIdleVehicles();

        scheduleUnplannedRequestsImpl();//reduce T_W (regular NOS)
    }
	
	private void initIdleVehicles()
    {
        idleVehicles = new HashSet<>();

        for (Vehicle veh : this.optimizerContext.fleet.getVehicles().values()) {
            if (this.scheduler.isIdle(veh)) {
                idleVehicles.add(veh);
            }
        }
    }
	
	private void scheduleUnplannedRequestsImpl()
    {
        Iterator<TaxiRequest> reqIter = unplannedRequests.iterator();
        while (reqIter.hasNext() && !idleVehicles.isEmpty()) {
            TaxiRequest req = reqIter.next();

//            Iterable<Vehicle> filteredVehs = idleVehicleFinder.filterVehiclesForRequest(idleVehicles,
//                    req);
            BestDispatchFinder.Dispatch<TaxiRequest> best = vrpFinder.findBestVehicleForRequest(req,
                    idleVehicles);

            if (best != null) {
                this.scheduler.scheduleRequest(best.vehicle, best.destination, best.path);
                reqIter.remove();
                idleVehicles.remove(best.vehicle);
            }
        }
    }
	
//	protected void scheduleRankReturn(Vehicle veh, double time, boolean charge, boolean home)
//    {
//        @SuppressWarnings("unchecked")
//        Schedule<Task> sched = (Schedule<Task>)veh.getSchedule();
//        TaxiStayTask last = (TaxiStayTask)Schedules.getLastTask(veh.getSchedule());
//        if (last.getStatus() != TaskStatus.STARTED)
//            throw new IllegalStateException();
//
//        last.setEndTime(time);
//        Link currentLink = last.getLink();
//        Link nearestRank = veh.getStartLink();
//
//        VrpPathWithTravelData path = VrpPathCalculator.calcPath(currentLink, nearestRank, time);
//        if (path.getArrivalTime() > veh.getT1())
//            return; // no rank return if vehicle is going out of service anyway
//        sched.addTask(new TaxiDriveTask(path));
//        sched.addTask(new TaxiStayTask(path.getArrivalTime(), veh.getT1(), nearestRank));
//
//    }
//
}
