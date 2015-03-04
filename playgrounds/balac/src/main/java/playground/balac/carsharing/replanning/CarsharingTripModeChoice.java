package playground.balac.carsharing.replanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.modules.ChangeSingleLegMode;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.population.algorithms.PlanAlgorithm;
/**
 * @author balacm
 */
public class CarsharingTripModeChoice extends AbstractMultithreadedModule{

	private final static Logger log = Logger.getLogger(ChangeSingleLegMode.class);

	/*package*/ final static String CONFIG_MODULE = "ft";
	/*package*/ final static String CONFIG_PARAM_MODES = "modes";
	/*package*/ final static String CONFIG_PARAM_IGNORECARAVAILABILITY = "ignoreCarAvailability";

	private String[] availableModes = new String[] { TransportMode.car, TransportMode.pt };
	private boolean ignoreCarAvailability = true;

	public CarsharingTripModeChoice(final Config config) {
		super(config.global().getNumberOfThreads());

		// try to get the modes from the "changeLegMode" module of the config file
		String modes = config.findParam(CONFIG_MODULE, CONFIG_PARAM_MODES);
		String ignorance = config.findParam(CONFIG_MODULE, CONFIG_PARAM_IGNORECARAVAILABILITY);

		// if there was anything in there, replace the default availableModes by the entries in the config file:
		if (modes != null) {
			String[] parts = StringUtils.explode(modes, ',');
			this.availableModes = new String[parts.length];
			for (int i = 0, n = parts.length; i < n; i++) {
				this.availableModes[i] = parts[i].trim().intern();
			}
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
