package playground.mzilske.d4d;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

public class DuplicatePlanStrategyFactory implements PlanStrategyFactory {

	@Override
	public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager eventsManager) {
		PlanStrategyImpl planStrategy = new PlanStrategyImpl(new RandomPlanSelector());
		planStrategy.addStrategyModule(new PlanStrategyModule() {

			@Override
			public void prepareReplanning(ReplanningContext replanningContext) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void handlePlan(Plan plan) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void finishReplanning() {
				// TODO Auto-generated method stub
				
			}
			
		});
		return planStrategy;
		
	}

}
