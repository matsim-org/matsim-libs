package playground.michalm.taxi.optimizer;

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.core.router.util.*;

import playground.michalm.taxi.scheduler.TaxiScheduler;


public class TaxiOptimizerContext
{
    public final MatsimVrpContext context;
    public final TravelTime travelTime;
    public final TravelDisutility travelDisutility;
    public final AbstractTaxiOptimizerParams optimizerParams;
    public final TaxiScheduler scheduler;


    public TaxiOptimizerContext(MatsimVrpContext context, TravelTime travelTime,
            TravelDisutility travelDisutility, AbstractTaxiOptimizerParams optimizerParams,
            TaxiScheduler scheduler)
    {
        this.context = context;
        this.travelTime = travelTime;
        this.travelDisutility = travelDisutility;
        this.optimizerParams = optimizerParams;
        this.scheduler = scheduler;
    }
}
