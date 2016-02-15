package playground.balac.aam.replanning;

import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author balacm
 */
public class AAMTripModeChoice extends AbstractMultithreadedModule{

	//private final static Logger log = Logger.getLogger(AAMTripModeChoice.class);

	/*package*/ final static String CONFIG_PARAM_IGNORECARAVAILABILITY = "ignoreCarAvailability";

	private String[] availableModes = null;
	private boolean ignoreCarAvailability = true;

	private final Provider<TripRouter> tripRouterProvider;

	public AAMTripModeChoice(final Config config, Provider<TripRouter> tripRouterProvider) {
		super(config.global().getNumberOfThreads());
		this.tripRouterProvider = tripRouterProvider;

		// try to get the modes from the "changeLegMode" module of the config file

		
		if (Boolean.parseBoolean(config.getModule("MovingPathways").getValue("useMovingPathways") )) {
			
			this.availableModes = new String[1];
			this.availableModes[0] = "movingpathways";
		}
			

		

	}
	
//	public AAMTripModeChoice(final int nOfThreads, final String[] modes, final boolean ignoreCarAvailabilty) {
//		super(nOfThreads);
//		this.availableModes = modes.clone();
//		this.ignoreCarAvailability = ignoreCarAvailabilty;
//	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		final TripRouter tripRouter = tripRouterProvider.get();
		ChooseRandomTripMode algo = new ChooseRandomTripMode(this.availableModes, MatsimRandom.getLocalInstance(), tripRouter.getStageActivityTypes());
		algo.setIgnoreCarAvailability(this.ignoreCarAvailability);
		return algo;
	}

}
