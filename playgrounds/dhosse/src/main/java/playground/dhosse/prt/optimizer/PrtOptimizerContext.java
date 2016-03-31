package playground.dhosse.prt.optimizer;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.*;

import playground.dhosse.prt.PrtConfig;


public class PrtOptimizerContext
    extends TaxiOptimizerContext
{
    public final PrtConfig prtConfigGroup;


    public PrtOptimizerContext(TaxiData taxiData, Scenario scenario, MobsimTimer timer,
            TravelTime travelTime, TravelDisutility travelDisutility, TaxiScheduler scheduler,
            PrtConfig prtConfigGroup)
    {
        super(taxiData, scenario, timer, travelTime, travelDisutility, scheduler);
        this.prtConfigGroup = prtConfigGroup;
    }
}
