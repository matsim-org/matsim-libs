package playground.balac.iduceddemand.strategies;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;

public class InsertRandomActivity extends AbstractMultithreadedModule {
	public InsertRandomActivity(final Scenario scenario) {
		super(scenario.getConfig().global().getNumberOfThreads());		
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		final TripRouter tripRouter = getReplanningContext().getTripRouter();
		ChooseActivityToInsert algo = new ChooseActivityToInsert(MatsimRandom.getLocalInstance(), tripRouter.getStageActivityTypes());

		return algo;
	}
}
