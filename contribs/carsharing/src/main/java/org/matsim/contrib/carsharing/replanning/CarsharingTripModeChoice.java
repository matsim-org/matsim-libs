package org.matsim.contrib.carsharing.replanning;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;

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

	public CarsharingTripModeChoice(Provider<TripRouter> tripRouterProvider, final Scenario scenario) {
		super(scenario.getConfig().global().getNumberOfThreads());
		this.tripRouterProvider = tripRouterProvider;

		// try to get the modes from the "changeLegMode" module of the config file

		this.scenario = scenario;
		// try to get the modes from the "changeLegMode" module of the config file

		
		if (Boolean.parseBoolean(this.scenario.getConfig().getModule("OneWayCarsharing").getValue("useOneWayCarsharing") )) {
			
			this.availableModes = new String[1];
			this.availableModes[0] = "onewaycarsharing";
		}
		if (Boolean.parseBoolean(this.scenario.getConfig().getModule("FreeFloating").getValue("useFreeFloating") )) {
			if (this.availableModes == null) {
				this.availableModes = new String[1];
				this.availableModes[0] = "freefloating";
			}
			else {
				this.availableModes = new String[2];
				this.availableModes[0] = "onewaycarsharing";
			this.availableModes[1] = "freefloating";
			}
		}
		
		if (!Boolean.parseBoolean(this.scenario.getConfig().getModule("FreeFloating").getValue("useFreeFloating") ) && !Boolean.parseBoolean(this.scenario.getConfig().getModule("OneWayCarsharing").getValue("useOneWayCarsharing") )) {
			this.availableModes = new String[1];
		}
				

		

	}
	
	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		final TripRouter tripRouter = tripRouterProvider.get();
		ChooseRandomTripMode algo = new ChooseRandomTripMode(this.scenario, this.availableModes, MatsimRandom.getLocalInstance(), tripRouter.getStageActivityTypes());
		return algo;
	}

}
