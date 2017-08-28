package taxibus.algorithm.optimizer;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.*;

import taxibus.algorithm.scheduler.TaxibusScheduler;
import taxibus.run.configuration.TaxibusConfigGroup;

public class TaxibusOptimizerContext {
	public final Fleet vrpData;
	public final Scenario scenario;
	public final MobsimTimer timer;
	public final TravelTime travelTime;
	public final TravelDisutility travelDisutility;
	public final TaxibusScheduler scheduler;

	public final TaxibusConfigGroup tbcg;

	public TaxibusOptimizerContext(Fleet vrpData, Scenario scenario, MobsimTimer timer, TravelTime travelTime,
			TravelDisutility travelDisutility, TaxibusScheduler scheduler, TaxibusConfigGroup tbcg) {
		this.vrpData = vrpData;
		this.scenario = scenario;
		this.timer = timer;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.scheduler = scheduler;

		this.tbcg = tbcg;
	}
}
