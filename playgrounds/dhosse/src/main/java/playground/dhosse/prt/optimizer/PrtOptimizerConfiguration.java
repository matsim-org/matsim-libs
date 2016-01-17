package playground.dhosse.prt.optimizer;

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.core.router.util.*;

import playground.dhosse.prt.PrtConfigGroup;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;
import playground.michalm.taxi.scheduler.TaxiScheduler;


public class PrtOptimizerConfiguration
    extends TaxiOptimizerConfiguration
{

    public final PrtConfigGroup prtConfigGroup;


    public PrtOptimizerConfiguration(MatsimVrpContext context, TravelTime travelTime,
            TravelDisutility travelDisutility, TaxiScheduler scheduler, int nearestRequestsLimit,
            int nearestVehiclesLimit, Goal goal, String workingDirectory, PrtConfigGroup prtConfigGroup)
    {
        super(context, travelTime, travelDisutility, scheduler, nearestRequestsLimit,
                nearestVehiclesLimit, goal, workingDirectory, null);

        this.prtConfigGroup = prtConfigGroup;
    }
}
