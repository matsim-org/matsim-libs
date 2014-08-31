package playground.balac.allcsmodestest.replanning.carsharingwithtaxi;

import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;
/**
 * @author balacm
 */
public class CarsharingWithTaxiTripModeChoice extends AbstractMultithreadedModule{

	private String[] availableModes = null;
	private boolean ignoreCarAvailability = true;

	public CarsharingWithTaxiTripModeChoice(final Config config) {
		super(config.global().getNumberOfThreads());

		// try to get the modes from the "changeLegMode" module of the config file

		
		if (Boolean.parseBoolean(config.getModule("OneWayCarsharing").getValue("useOneWayCarsharing") )) {
			
			this.availableModes = new String[2];
			this.availableModes[0] = "taxi";
			this.availableModes[1] = "onewaycarsharing";
		}
		if (Boolean.parseBoolean(config.getModule("FreeFloating").getValue("useFreeFloating") )) {
			if (this.availableModes == null) {
				this.availableModes = new String[2];
				this.availableModes[0] = "taxi";
				this.availableModes[1] = "freefloating";
			}
			else {
				this.availableModes = new String[3];
				this.availableModes[0] = "taxi";
				this.availableModes[1] = "onewaycarsharing";
				this.availableModes[2] = "freefloating";
			}
		}
		
		if (!Boolean.parseBoolean(config.getModule("FreeFloating").getValue("useFreeFloating") ) && !Boolean.parseBoolean(config.getModule("OneWayCarsharing").getValue("useOneWayCarsharing") )) {
			this.availableModes = new String[1];
			this.availableModes[0] = "taxi";
		}
			
	}
	
	public CarsharingWithTaxiTripModeChoice(final int nOfThreads, final String[] modes, final boolean ignoreCarAvailabilty) {
		super(nOfThreads);
		this.availableModes = modes.clone();
		this.ignoreCarAvailability = ignoreCarAvailabilty;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		final TripRouter tripRouter = getReplanningContext().getTripRouter();
		ChooseRandomTripModeWithTaxi algo = new ChooseRandomTripModeWithTaxi(this.availableModes, MatsimRandom.getLocalInstance(), tripRouter.getStageActivityTypes());
		algo.setIgnoreCarAvailability(this.ignoreCarAvailability);
		return algo;
	}

}
