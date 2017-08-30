package org.matsim.contrib.carsharing.replanning;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipContainer;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

/**
 * @author balacm
 */
public class CarsharingTripModeChoice extends AbstractMultithreadedModule{

	/*package*/ final static String CONFIG_MODULE = "ft";
	/*package*/ final static String CONFIG_PARAM_MODES = "modes";
	/*package*/ final static String CONFIG_PARAM_IGNORECARAVAILABILITY = "ignoreCarAvailability";

	private final Provider<TripRouter> tripRouterProvider;

	private String[] availableModes = null;
	private final Scenario scenario;
	private MembershipContainer memberships; 
	public CarsharingTripModeChoice(Provider<TripRouter> tripRouterProvider, final Scenario scenario, MembershipContainer memberships ) {
		super(scenario.getConfig().global().getNumberOfThreads());
		this.tripRouterProvider = tripRouterProvider;
		this.memberships = memberships;
		// try to get the modes from the "changeLegMode" module of the config file

		this.scenario = scenario;
		// try to get the modes from the "changeLegMode" module of the config file

		
		boolean useOneway = Boolean.parseBoolean(this.scenario.getConfig().getModule("OneWayCarsharing").getValue("useOneWayCarsharing"));
		boolean useFf = Boolean.parseBoolean(this.scenario.getConfig().getModule("FreeFloating").getValue("useFreeFloating"));
		boolean useBs = Boolean.parseBoolean(this.scenario.getConfig().getModule("Bikeshare").getValue("useBikeshare"));

		List<String> modes = new ArrayList<String>();
		
		if (useOneway)
			modes.add("oneway");
		if (useFf)
			modes.add("freefloating");
		if (useBs)
			modes.add("bikeshare");
		
		
		if (!useBs && !useOneway && !useFf) {
			this.availableModes = new String[1];
		}
		else
			this.availableModes = (String[]) modes.toArray();
	}
	
	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		final TripRouter tripRouter = tripRouterProvider.get();
		ChooseRandomTripMode algo = new ChooseRandomTripMode(this.scenario, this.availableModes,
				MatsimRandom.getLocalInstance(), tripRouter.getStageActivityTypes(), this.memberships);
		return algo;
	}

}
