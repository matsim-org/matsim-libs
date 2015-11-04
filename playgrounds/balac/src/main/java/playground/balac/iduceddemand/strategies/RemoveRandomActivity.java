package playground.balac.iduceddemand.strategies;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;

public class RemoveRandomActivity  extends AbstractMultithreadedModule {
	private Scenario scenario;

	public RemoveRandomActivity(Scenario scenario) {
		super(scenario.getConfig().global().getNumberOfThreads());
		this.scenario = scenario;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		final TripRouter tripRouter = getReplanningContext().getTripRouter();
		ChooseActivityToRemove algo = new ChooseActivityToRemove(MatsimRandom.getLocalInstance(), tripRouter.getStageActivityTypes(),
				this.scenario);

		return algo;
	}

}
