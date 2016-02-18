package playground.balac.induceddemand.strategies;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;

import javax.inject.Provider;


public class RandomActivitySwaper extends AbstractMultithreadedModule {

	private final Provider<TripRouter> tripRouterProvider;

	private Scenario scenario;
	public RandomActivitySwaper(Provider<TripRouter> tripRouterProvider, final Scenario scenario) {
		super(scenario.getConfig().global().getNumberOfThreads());
		this.tripRouterProvider = tripRouterProvider;
		this.scenario = scenario;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		final TripRouter tripRouter = tripRouterProvider.get();
		ChooseRandomActivitiesToSwap algo = new ChooseRandomActivitiesToSwap(scenario,
				MatsimRandom.getLocalInstance(), tripRouter.getStageActivityTypes());

		return algo;
	}
}
