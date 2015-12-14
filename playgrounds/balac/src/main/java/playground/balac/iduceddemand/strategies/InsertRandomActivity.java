package playground.balac.iduceddemand.strategies;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.population.algorithms.PlanAlgorithm;

import javax.inject.Inject;
import javax.inject.Provider;

public class InsertRandomActivity extends AbstractMultithreadedModule {
	private final Provider<TripRouter> tripRouterProvider;

	private Scenario scenario;
	private final QuadTree shopFacilityQuadTree;
	private final QuadTree leisureFacilityQuadTree;

	public InsertRandomActivity(Provider<TripRouter> tripRouterProvider, final Scenario scenario, QuadTree shopFacilityQuadTree,
								QuadTree leisureFacilityQuadTree) {
		super(scenario.getConfig().global().getNumberOfThreads());
		this.tripRouterProvider = tripRouterProvider;
		this.scenario = scenario;
		this.shopFacilityQuadTree = shopFacilityQuadTree;
		this.leisureFacilityQuadTree = leisureFacilityQuadTree;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		final TripRouter tripRouter = tripRouterProvider.get();
		ChooseActivityToInsert algo = new ChooseActivityToInsert(MatsimRandom.getLocalInstance(), tripRouter.getStageActivityTypes(),
				this.scenario, shopFacilityQuadTree, leisureFacilityQuadTree);

		return algo;
	}
}
