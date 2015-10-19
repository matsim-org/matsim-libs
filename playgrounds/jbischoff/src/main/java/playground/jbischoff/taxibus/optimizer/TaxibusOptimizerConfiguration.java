package playground.jbischoff.taxibus.optimizer;

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.core.router.util.*;

import playground.jbischoff.taxibus.optimizer.filter.TaxibusFilterFactory;
import playground.jbischoff.taxibus.scheduler.TaxibusScheduler;


public class TaxibusOptimizerConfiguration
{
    public final MatsimVrpContext context;

    public final TravelTime travelTime;
    public final TravelDisutility travelDisutility;

    public final TaxibusScheduler scheduler;
    public final TaxibusFilterFactory filterFactory;

    public final Goal goal;

    public final String workingDirectory;


    public static enum Goal
    {
        MIN_WAIT_TIME, MIN_PICKUP_TIME, DEMAND_SUPPLY_EQUIL, NULL
    };


    public TaxibusOptimizerConfiguration(MatsimVrpContext context, TravelTime travelTime,
            TravelDisutility travelDisutility, TaxibusScheduler scheduler,
            TaxibusFilterFactory filterFactory, Goal goal, String workingDirectory)
    {
        this.context = context;

        this.travelTime = travelTime;
        this.travelDisutility = travelDisutility;

        this.scheduler = scheduler;
        this.filterFactory = filterFactory;

        this.goal = goal;

        this.workingDirectory = workingDirectory;
    }
}
