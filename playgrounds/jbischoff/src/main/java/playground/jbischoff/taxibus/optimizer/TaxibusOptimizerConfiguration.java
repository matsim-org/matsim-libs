package playground.jbischoff.taxibus.optimizer;

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.path.VrpPathCalculator;

import playground.jbischoff.taxibus.optimizer.filter.TaxibusFilterFactory;
import playground.jbischoff.taxibus.scheduler.TaxibusScheduler;
import playground.jbischoff.taxibus.vehreqpath.TaxibusVehicleRequestPathFinder;
import playground.michalm.taxi.vehreqpath.VehicleRequestPathCost;
import playground.michalm.taxi.vehreqpath.VehicleRequestPaths;


public class TaxibusOptimizerConfiguration 
{
	   public final MatsimVrpContext context;

	    public final VrpPathCalculator calculator;
	    public final TaxibusScheduler scheduler;
	    public final TaxibusVehicleRequestPathFinder vrpFinder;
	    public final TaxibusFilterFactory filterFactory;

	    public final Goal goal;

	    public final String workingDirectory;


    public static enum Goal
    {
        MIN_WAIT_TIME, MIN_PICKUP_TIME, DEMAND_SUPPLY_EQUIL, NULL
    };

    public TaxibusOptimizerConfiguration(MatsimVrpContext context, VrpPathCalculator calculator,
    		TaxibusScheduler scheduler, TaxibusVehicleRequestPathFinder vrpFinder,
    		TaxibusFilterFactory filterFactory, Goal goal, String workingDirectory)
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
