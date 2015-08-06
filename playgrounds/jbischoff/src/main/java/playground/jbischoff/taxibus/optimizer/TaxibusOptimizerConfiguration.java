package playground.jbischoff.taxibus.optimizer;

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;

import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;
import playground.michalm.taxi.optimizer.filter.FilterFactory;
import playground.michalm.taxi.scheduler.TaxiScheduler;
import playground.michalm.taxi.vehreqpath.*;


public class TaxibusOptimizerConfiguration extends TaxiOptimizerConfiguration
{
    


    public static enum Goal
    {
        MIN_WAIT_TIME, MIN_PICKUP_TIME, DEMAND_SUPPLY_EQUIL, NULL
    };

    public TaxibusOptimizerConfiguration(MatsimVrpContext context, VrpPathCalculator calculator,
            TaxiScheduler scheduler, VehicleRequestPathFinder vrpFinder,
            FilterFactory filterFactory, Goal goal, String workingDirectory)
    {
       super (context, calculator, scheduler, vrpFinder, filterFactory, null, workingDirectory, null);
       
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
