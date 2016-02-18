package playground.balac.induceddemand.strategies;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.population.algorithms.PlanAlgorithm;

import javax.inject.Provider;

public class InsertRandomActivity extends AbstractMultithreadedModule {
	
	private Scenario scenario;
	private final QuadTree shopFacilityQuadTree;
	private final QuadTree leisureFacilityQuadTree;

	private final Provider<TripRouter> tripRouterProvider;

	public InsertRandomActivity(final Scenario scenario, QuadTree shopFacilityQuadTree,
								QuadTree leisureFacilityQuadTree, Provider<TripRouter> tripRouterProvider) {
		super(scenario.getConfig().global().getNumberOfThreads());
		this.scenario = scenario;
		this.shopFacilityQuadTree = shopFacilityQuadTree;
		this.leisureFacilityQuadTree = leisureFacilityQuadTree;
		this.tripRouterProvider = tripRouterProvider;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		final TripRouter tripRouter = tripRouterProvider.get();
		ChooseActivityToInsert algo = new ChooseActivityToInsert(MatsimRandom.getLocalInstance(), tripRouter.getStageActivityTypes(),
				this.scenario, shopFacilityQuadTree, leisureFacilityQuadTree);

		return algo;
	}
}
