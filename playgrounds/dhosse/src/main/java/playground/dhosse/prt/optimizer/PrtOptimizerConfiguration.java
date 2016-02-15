package playground.dhosse.prt.optimizer;

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.router.util.*;

import playground.dhosse.prt.PrtConfigGroup;


public class PrtOptimizerConfiguration
    extends TaxiOptimizerContext
{

    public final PrtConfigGroup prtConfigGroup;


    public PrtOptimizerConfiguration(MatsimVrpContext context, TravelTime travelTime,
            TravelDisutility travelDisutility, AbstractTaxiOptimizerParams optimizerParams, TaxiScheduler scheduler,
            PrtConfigGroup prtConfigGroup)
    {
        super(context, travelTime, travelDisutility, null, scheduler);
        this.prtConfigGroup = prtConfigGroup;
    }
}
