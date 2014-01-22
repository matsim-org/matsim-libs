package playground.balac.freefloating.replanning;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;


public class FreeFloatingTripModeChoice extends AbstractMultithreadedModule{




	private String[] availableModes = new String[] { TransportMode.car, TransportMode.pt, TransportMode.bike, TransportMode.walk };
	private boolean ignoreCarAvailability = false;

	public FreeFloatingTripModeChoice(final Config config) {
		super(config.global().getNumberOfThreads());

		

	}
	
	public FreeFloatingTripModeChoice(final int nOfThreads, final String[] modes, final boolean ignoreCarAvailabilty) {
		super(nOfThreads);
		this.availableModes = modes.clone();
		this.ignoreCarAvailability = ignoreCarAvailabilty;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		final TripRouter tripRouter = getReplanningContext().getTripRouter();
		FreeFloatingChooseRandomTripMode algo = new FreeFloatingChooseRandomTripMode(this.availableModes, MatsimRandom.getLocalInstance(), tripRouter.getStageActivityTypes());
		algo.setIgnoreCarAvailability(this.ignoreCarAvailability);
		return algo;
	}

}
