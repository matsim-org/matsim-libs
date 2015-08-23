package playground.michalm.taxi.optimizer;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;

import playground.michalm.taxi.optimizer.filter.FilterFactory;
import playground.michalm.taxi.scheduler.TaxiScheduler;
import playground.michalm.taxi.vehreqpath.*;
import playground.michalm.zone.Zone;


public class TaxiOptimizerConfiguration
{
    public final MatsimVrpContext context;

    public final VrpPathCalculator calculator;
    public final TaxiScheduler scheduler;
    public final VehicleRequestPathFinder vrpFinder;
    public final FilterFactory filterFactory;
    public final Map<Id<Zone>, Zone> zones;

    public final Goal goal;

    public final String workingDirectory;


    public static enum Goal
    {
        MIN_WAIT_TIME, MIN_PICKUP_TIME, DEMAND_SUPPLY_EQUIL, NULL
    };


    public TaxiOptimizerConfiguration(MatsimVrpContext context, VrpPathCalculator calculator,
            TaxiScheduler scheduler, VehicleRequestPathFinder vrpFinder,
            FilterFactory filterFactory, Goal goal, String workingDirectory,
            Map<Id<Zone>, Zone> zones)
    {
        this.context = context;

        this.calculator = calculator;
        this.scheduler = scheduler;
        this.vrpFinder = vrpFinder;
        this.filterFactory = filterFactory;
        this.zones = zones;

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
