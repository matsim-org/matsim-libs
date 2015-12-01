package playground.michalm.taxi.optimizer;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.core.router.util.*;

import playground.michalm.taxi.optimizer.filter.FilterFactory;
import playground.michalm.taxi.scheduler.TaxiScheduler;
import playground.michalm.zone.Zone;


public class TaxiOptimizerConfiguration
{
    public final MatsimVrpContext context;

    public final TravelTime travelTime;
    public final TravelDisutility travelDisutility;

    public final TaxiScheduler scheduler;
    public final FilterFactory filterFactory;
    public final Map<Id<Zone>, Zone> zones;

    public final Goal goal;

    public final String workingDirectory;


    public static enum Goal
    {
        MIN_WAIT_TIME, MIN_PICKUP_TIME, DEMAND_SUPPLY_EQUIL, NULL
    };


    public TaxiOptimizerConfiguration(MatsimVrpContext context, TravelTime travelTime,
            TravelDisutility travelDisutility, TaxiScheduler scheduler, FilterFactory filterFactory,
            Goal goal, String workingDirectory, Map<Id<Zone>, Zone> zones)
    {
        this.context = context;

        this.travelTime = travelTime;
        this.travelDisutility = travelDisutility;

        this.scheduler = scheduler;
        this.filterFactory = filterFactory;
        this.zones = zones;

        this.goal = goal;

        this.workingDirectory = workingDirectory;
    }
}
