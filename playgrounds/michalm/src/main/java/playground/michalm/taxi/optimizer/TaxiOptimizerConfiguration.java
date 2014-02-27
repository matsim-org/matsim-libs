package playground.michalm.taxi.optimizer;

import java.util.Comparator;

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;

import playground.michalm.taxi.scheduler.TaxiScheduler;
import playground.michalm.taxi.vehreqpath.*;


public class TaxiOptimizerConfiguration
{
    public final MatsimVrpContext context;

    public final VrpPathCalculator calculator;
    public final TaxiScheduler scheduler;
    public final VehicleRequestPathFinder vrpFinder;

    public final Goal goal;


    public static enum Goal
    {
        MIN_WAIT_TIME, MIN_PICKUP_TIME, DEMAND_SUPPLY_EQUIL, NULL
    };


    public TaxiOptimizerConfiguration(MatsimVrpContext context, VrpPathCalculator calculator,
            TaxiScheduler scheduler, VehicleRequestPathFinder vrpFinder, Goal goal)
    {
        this.context = context;

        this.calculator = calculator;
        this.scheduler = scheduler;
        this.vrpFinder = vrpFinder;

        this.goal = goal;
    }


    public Comparator<VehicleRequestPath> getVrpComparator()
    {
        switch (goal) {
            case MIN_WAIT_TIME:
                return VehicleRequestPaths.TW_COMPARATOR;

            case MIN_PICKUP_TIME:
                return VehicleRequestPaths.TP_COMPARATOR;

            case NULL:
                return null;

            default:
                throw new IllegalStateException();
        }
    }
}
