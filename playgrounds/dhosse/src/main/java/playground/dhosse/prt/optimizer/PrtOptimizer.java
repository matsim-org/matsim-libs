package playground.dhosse.prt.optimizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;
import org.matsim.contrib.dvrp.router.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;

import playground.jbischoff.taxi.optimizer.rank.IdleRankVehicleFinder;
import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.scheduler.TaxiScheduler;
import playground.michalm.taxi.vehreqpath.VehicleRequestPath;
import playground.michalm.taxi.vehreqpath.VehicleRequestPaths;

public class PrtOptimizer implements VrpOptimizerWithOnlineTracking, MobsimBeforeSimStepListener {

	protected final IdleRankVehicleFinder idleVehicleFinder;
	protected final Collection<TaxiRequest> unplannedRequests;
	
	protected boolean requiresReoptimization = false;
	
	private Set<Vehicle> idleVehicles;
	
	private final MatsimVrpContext context;
	private final TaxiScheduler scheduler;
	private VrpPathCalculator calculator;
	private TaxiOptimizerConfiguration optimizerConfig;
	
	public PrtOptimizer(TaxiOptimizerConfiguration optimizerConfig){
		
		this(optimizerConfig.context, optimizerConfig, optimizerConfig.calculator, optimizerConfig.scheduler, new IdleRankVehicleFinder(optimizerConfig.context,
						optimizerConfig.scheduler));
		
	}
	
	protected PrtOptimizer(MatsimVrpContext context, TaxiOptimizerConfiguration optimizerConfig, VrpPathCalculator calculator, TaxiScheduler scheduler, IdleRankVehicleFinder vehicleFinder){
		
		this.optimizerConfig = optimizerConfig;
		this.context = context;
		this.scheduler = scheduler;
		this.calculator = calculator; 
		this.idleVehicleFinder = vehicleFinder;
		this.unplannedRequests = new ArrayList<TaxiRequest>() {
		};
		
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
	public void nextTask(Schedule<? extends Task> schedule) {
		
		@SuppressWarnings("unchecked")
		Schedule<TaxiTask> prtSchedule = (Schedule<TaxiTask>)schedule;

        this.scheduler.updateBeforeNextTask(prtSchedule);
        TaxiTask newCurrentTask = prtSchedule.nextTask();

        if (newCurrentTask != null // schedule != COMPLETED
                && newCurrentTask.getTaxiTaskType() == TaxiTask.TaxiTaskType.STAY) {
            requiresReoptimization = true;
        }

	}
	
	@Override
	public void nextLinkEntered(DriveTask driveTask)
	{
		scheduler.updateTimeline(TaxiSchedules.asTaxiSchedule(driveTask.getSchedule()));
	}
	
	protected void scheduleUnplannedRequests()
    {
        initIdleVehicles();

        scheduleUnplannedRequestsImpl();//reduce T_W (regular NOS)
        
    }
	
	private void initIdleVehicles()
    {
        idleVehicles = new HashSet<>();

        for (Vehicle veh : this.context.getVrpData().getVehicles().values()) {
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

            Iterable<Vehicle> filteredVehs = idleVehicleFinder.filterVehiclesForRequest(idleVehicles,
                    req);
            VehicleRequestPath best = this.optimizerConfig.vrpFinder.findBestVehicleForRequest(req,
                    filteredVehs, VehicleRequestPaths.TW_COST);

            if (best != null) {
                this.scheduler.scheduleRequest(best);
                reqIter.remove();
                idleVehicles.remove(best.vehicle);
            }
        }
    }
	
	protected void scheduleRankReturn(Vehicle veh, double time, boolean charge, boolean home)
    {
        @SuppressWarnings("unchecked")
        Schedule<Task> sched = (Schedule<Task>)veh.getSchedule();
        TaxiStayTask last = (TaxiStayTask)Schedules.getLastTask(veh.getSchedule());
        if (last.getStatus() != TaskStatus.STARTED)
            throw new IllegalStateException();

        last.setEndTime(time);
        Link currentLink = last.getLink();
        Link nearestRank = veh.getStartLink();

        VrpPathWithTravelData path = this.calculator
                .calcPath(currentLink, nearestRank, time);
        if (path.getArrivalTime() > veh.getT1())
            return; // no rank return if vehicle is going out of service anyway
        sched.addTask(new TaxiDriveTask(path));
        sched.addTask(new TaxiStayTask(path.getArrivalTime(), veh.getT1(), nearestRank));

    }

}
