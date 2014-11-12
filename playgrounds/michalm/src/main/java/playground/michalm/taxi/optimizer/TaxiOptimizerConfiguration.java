package playground.michalm.taxi.optimizer;

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;

import playground.michalm.taxi.optimizer.filter.FilterParams;
import playground.michalm.taxi.scheduler.TaxiScheduler;
import playground.michalm.taxi.vehreqpath.*;


public class TaxiOptimizerConfiguration
{
    public final MatsimVrpContext context;

    public final VrpPathCalculator calculator;
    public final TaxiScheduler scheduler;
    public final VehicleRequestPathFinder vrpFinder;
    public final FilterParams filterParams;

    public final Goal goal;

    public final String workingDirectory;


    public static enum Goal
    {
        MIN_WAIT_TIME, MIN_PICKUP_TIME, DEMAND_SUPPLY_EQUIL, NULL
    };


    public TaxiOptimizerConfiguration(MatsimVrpContext context, VrpPathCalculator calculator,
            TaxiScheduler scheduler, VehicleRequestPathFinder vrpFinder, FilterParams filterParams,
            Goal goal, String workingDirectory)
    {
        this.context = context;

        this.calculator = calculator;
        this.scheduler = scheduler;
        this.vrpFinder = vrpFinder;
        this.filterParams = filterParams;

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
