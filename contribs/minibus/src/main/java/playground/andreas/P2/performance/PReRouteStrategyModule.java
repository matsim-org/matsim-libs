package playground.andreas.P2.performance;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.ReplanningContext;

public class PReRouteStrategyModule implements PlanStrategyModule{
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(PReRouteStrategyModule.class);

	private Scenario scenario;
	private PPlanRouter planRouter;

	public PReRouteStrategyModule(Scenario scenario) {
		this.scenario = (Scenario) scenario;
	}

	@Override
	public void finishReplanning() {
	}

	@Override
	public void handlePlan(Plan plan) {
		this.planRouter.run(plan);
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
		this.planRouter = new PPlanRouter(
				replanningContext.getTripRouter(),
				this.scenario.getActivityFacilities());
	}

}
