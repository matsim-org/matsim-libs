package playground.jbischoff.taxibus.algorithm.optimizer;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.*;

import playground.jbischoff.taxibus.algorithm.scheduler.TaxibusScheduler;
import playground.jbischoff.taxibus.run.configuration.TaxibusConfigGroup;


public class TaxibusOptimizerContext
{
    public final VrpData vrpData;
    public final Scenario scenario;
    public final MobsimTimer timer;
    public final TravelTime travelTime;
    public final TravelDisutility travelDisutility;
    public final TaxibusScheduler scheduler;

    public final String workingDirectory;
    public final TaxibusConfigGroup tbcg;


    public TaxibusOptimizerContext(VrpData vrpData, Scenario scenario, MobsimTimer timer,
            TravelTime travelTime, TravelDisutility travelDisutility, TaxibusScheduler scheduler,
            String workingDirectory, TaxibusConfigGroup tbcg)
    {
        this.vrpData = vrpData;
        this.scenario = scenario;
        this.timer = timer;
        this.travelTime = travelTime;
        this.travelDisutility = travelDisutility;
        this.scheduler = scheduler;

        this.workingDirectory = workingDirectory;
        this.tbcg = tbcg;
    }
}
