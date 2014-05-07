package playground.balac.allcsmodestest.replanning;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.modules.ChangeSingleLegMode;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;
/**
 * @author balacm
 */
public class CarsharingTripModeChoice extends AbstractMultithreadedModule{

	private final static Logger log = Logger.getLogger(ChangeSingleLegMode.class);

	/*package*/ final static String CONFIG_MODULE = "ft";
	/*package*/ final static String CONFIG_PARAM_MODES = "modes";
	/*package*/ final static String CONFIG_PARAM_IGNORECARAVAILABILITY = "ignoreCarAvailability";

	private String[] availableModes = null;
	private boolean ignoreCarAvailability = true;

	public CarsharingTripModeChoice(final Config config) {
		super(config.global().getNumberOfThreads());

		// try to get the modes from the "changeLegMode" module of the config file
		String ignorance = config.findParam(CONFIG_MODULE, CONFIG_PARAM_IGNORECARAVAILABILITY);

		
		if (Boolean.parseBoolean(config.getModule("OneWayCarsharing").getValue("useOneWayCarsharing") )) {
			
			this.availableModes = new String[1];
			this.availableModes[0] = "onewaycarsharing";
		}
		if (Boolean.parseBoolean(config.getModule("FreeFloating").getValue("useFreeFloating") )) {
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
		
		if (!Boolean.parseBoolean(config.getModule("FreeFloating").getValue("useFreeFloating") ) && !Boolean.parseBoolean(config.getModule("OneWayCarsharing").getValue("useOneWayCarsharing") )) {
			this.availableModes = new String[1];
		}
				

		if (ignorance != null) {
			this.ignoreCarAvailability = Boolean.parseBoolean(ignorance);
			log.info("using ignoreCarAvailability from configuration: " + this.ignoreCarAvailability);
		}

	}
	
	public CarsharingTripModeChoice(final int nOfThreads, final String[] modes, final boolean ignoreCarAvailabilty) {
		super(nOfThreads);
		this.availableModes = modes.clone();
		this.ignoreCarAvailability = ignoreCarAvailabilty;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		final TripRouter tripRouter = getReplanningContext().getTripRouter();
		ChooseRandomTripMode algo = new ChooseRandomTripMode(this.availableModes, MatsimRandom.getLocalInstance(), tripRouter.getStageActivityTypes());
		algo.setIgnoreCarAvailability(this.ignoreCarAvailability);
		return algo;
	}

}
