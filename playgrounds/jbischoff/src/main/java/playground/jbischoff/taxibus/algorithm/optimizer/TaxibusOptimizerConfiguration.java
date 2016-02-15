package playground.jbischoff.taxibus.algorithm.optimizer;

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.core.router.util.*;

import playground.jbischoff.taxibus.algorithm.scheduler.TaxibusScheduler;
import playground.jbischoff.taxibus.run.configuration.TaxibusConfigGroup;

public class TaxibusOptimizerConfiguration {
	public final MatsimVrpContext context;

	public final TravelTime travelTime;
	public final TravelDisutility travelDisutility;

	public final TaxibusScheduler scheduler;

	public final String workingDirectory;

	public final TaxibusConfigGroup tbcg;
	public TaxibusOptimizerConfiguration(MatsimVrpContext context, TravelTime travelTime,
			TravelDisutility travelDisutility, TaxibusScheduler scheduler, String workingDirectory, TaxibusConfigGroup tbcg) {
		this.context = context;

		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.tbcg = tbcg;
		this.scheduler = scheduler;

		this.workingDirectory = workingDirectory;
	}
}
