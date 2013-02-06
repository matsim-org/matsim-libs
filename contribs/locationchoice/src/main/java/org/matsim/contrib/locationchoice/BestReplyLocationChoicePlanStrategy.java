package org.matsim.contrib.locationchoice;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.bestresponse.LocationChoiceBestResponseContext;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.LocationChoiceConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

public class BestReplyLocationChoicePlanStrategy implements PlanStrategy {

	private PlanStrategyImpl delegate;
	
//	private static int locachoiceWrnCnt;
	
	public BestReplyLocationChoicePlanStrategy(LocationChoiceBestResponseContext lcContext) {
		if ( !LocationChoiceConfigGroup.Algotype.bestResponse.equals(lcContext.getScenario().getConfig().locationchoice().getAlgorithm())) {
			throw new RuntimeException("wrong class for selected location choice algorithm type; aborting ...") ;
		}
		Config config = lcContext.getScenario().getConfig() ;
		String planSelector = config.locationchoice().getPlanSelector();
		if (planSelector.equals("BestScore")) {
			delegate = new PlanStrategyImpl(new BestPlanSelector());
		} else if (planSelector.equals("ChangeExpBeta")) {
			delegate = new PlanStrategyImpl(new ExpBetaPlanChanger(config.planCalcScore().getBrainExpBeta()));
		} else if (planSelector.equals("SelectRandom")) {
			delegate = new PlanStrategyImpl(new RandomPlanSelector());
		} else {
			delegate = new PlanStrategyImpl(new ExpBetaPlanSelector(config.planCalcScore()));
		}
		delegate.addStrategyModule(new BestReplyLocationChoice(lcContext));
		delegate.addStrategyModule(new ReRoute(lcContext.getScenario()));
	}
	
	@Override
	public void run(Person person) {
		delegate.run(person);
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		delegate.init(replanningContext);
	}

	@Override
	public void finish() {
		delegate.finish();
	}
	
}
