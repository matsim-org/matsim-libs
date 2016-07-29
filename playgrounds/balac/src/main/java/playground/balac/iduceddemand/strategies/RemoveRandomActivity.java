package playground.balac.iduceddemand.strategies;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;

import javax.inject.Inject;
import javax.inject.Provider;

public class RemoveRandomActivity  extends AbstractMultithreadedModule {


	private final Provider<TripRouter> tripRouterProvider;

	public RemoveRandomActivity(Scenario scenario, Provider<TripRouter> tripRouterProvider) {
		super(scenario.getConfig().global().getNumberOfThreads());
		this.tripRouterProvider = tripRouterProvider;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		final TripRouter tripRouter = tripRouterProvider.get();
		ChooseActivityToRemove algo = new ChooseActivityToRemove(MatsimRandom.getLocalInstance(), tripRouter.getStageActivityTypes());

		return algo;
	}

}
