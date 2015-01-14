package playground.dhosse.prt.optimizer;

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;

import playground.michalm.taxi.optimizer.filter.FilterFactory;
import playground.michalm.taxi.scheduler.TaxiScheduler;
import playground.michalm.taxi.vehreqpath.VehicleRequestPathCost;
import playground.michalm.taxi.vehreqpath.VehicleRequestPathFinder;
import playground.michalm.taxi.vehreqpath.VehicleRequestPaths;

public class PrtOptimizerConfiguration {
	
	public final MatsimVrpContext context;

    public final VrpPathCalculator calculator;
    public final TaxiScheduler scheduler;
    public final VehicleRequestPathFinder vrpFinder;
    public final FilterFactory filterFactory;

    public final Goal goal;

    public final String workingDirectory;


    public static enum Goal
    {
        MIN_WAIT_TIME, MIN_PICKUP_TIME, DEMAND_SUPPLY_EQUIL, NULL
    };
    
    public PrtOptimizerConfiguration(MatsimVrpContext context, VrpPathCalculator calculator,
            TaxiScheduler scheduler, playground.michalm.taxi.vehreqpath.VehicleRequestPathFinder vrpFinder,
            FilterFactory filterFactory, Goal goal, String workingDirectory)
    {
    	
    	this.context = context;

        this.calculator = calculator;
        this.scheduler = scheduler;
        this.vrpFinder = vrpFinder;
        this.filterFactory = filterFactory;

        this.goal = goal;

        this.workingDirectory = workingDirectory;
    	
    }
    
    public VehicleRequestPathCost getVrpCost()
    {
        switch (goal) {
            case MIN_WAIT_TIME:
                return VehicleRequestPaths.TW_COST;

            case MIN_PICKUP_TIME:
                return VehicleRequestPaths.TP_COST;

            case NULL:
                return null;

            default:
                throw new IllegalStateException();
        }
    }

}
