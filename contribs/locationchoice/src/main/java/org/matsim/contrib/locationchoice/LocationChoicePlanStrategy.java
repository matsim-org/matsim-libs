package org.matsim.contrib.locationchoice;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspExperimentalConfigKey;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

public class LocationChoicePlanStrategy implements PlanStrategy {

	private PlanStrategyImpl delegate;
	
//	private static int locachoiceWrnCnt;
	
	public LocationChoicePlanStrategy(Scenario scenario) {
		String planSelector = scenario.getConfig().locationchoice().getPlanSelector();
		if (planSelector.equals("BestScore")) {
			delegate = new PlanStrategyImpl(new BestPlanSelector());
		} else if (planSelector.equals("ChangeExpBeta")) {
			delegate = new PlanStrategyImpl(new ExpBetaPlanChanger(scenario.getConfig().planCalcScore().getBrainExpBeta()));
		} else if (planSelector.equals("SelectRandom")) {
			delegate = new PlanStrategyImpl(new RandomPlanSelector());
		} else {
			delegate = new PlanStrategyImpl(new ExpBetaPlanSelector(scenario.getConfig().planCalcScore()));
		}
		delegate.addStrategyModule(new LocationChoice(scenario));
		delegate.addStrategyModule(new ReRoute(scenario));
//		delegate.addStrategyModule(new TimeAllocationMutator(scenario.getConfig()));
//		if ( locachoiceWrnCnt < 1 ) {
//			locachoiceWrnCnt ++ ;
//			Logger.getLogger("dummy").warn("I don't think that using TimeAllocationMutator as last step of locationchoice" +
//					" (or of any strategy, for that matter) makes sense. --> please remove from code.   kai, oct'12") ;
//			if (scenario.getConfig().vspExperimental().getValue(VspExperimentalConfigKey.vspDefaultsCheckingLevel).equals(VspExperimentalConfigGroup.ABORT) ) {
//				throw new RuntimeException("will not use locachoice followed by TimeMutation within VSP. Aborting ...") ;
//			}
//		}
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
