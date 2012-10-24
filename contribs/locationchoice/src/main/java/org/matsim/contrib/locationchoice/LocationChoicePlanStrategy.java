package org.matsim.contrib.locationchoice;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspExperimentalConfigKey;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

public class LocationChoicePlanStrategy implements PlanStrategy {

	private PlanStrategyImpl delegate;
	
	private static int locachoiceWrnCnt;
	
	public LocationChoicePlanStrategy(Controler controler) {
		String planSelector = controler.getScenario().getConfig().locationchoice().getPlanSelector();
		if (planSelector.equals("BestScore")) {
			delegate = new PlanStrategyImpl(new BestPlanSelector());
		} else if (planSelector.equals("ChangeExpBeta")) {
			delegate = new PlanStrategyImpl(new ExpBetaPlanChanger(controler.getScenario().getConfig().planCalcScore().getBrainExpBeta()));
		} else if (planSelector.equals("SelectRandom")) {
			delegate = new PlanStrategyImpl(new RandomPlanSelector());
		} else {
			delegate = new PlanStrategyImpl(new ExpBetaPlanSelector(controler.getScenario().getConfig().planCalcScore()));
		}
		delegate.addStrategyModule(new LocationChoice(controler.getNetwork(), controler));
		delegate.addStrategyModule(new ReRoute(controler));
		delegate.addStrategyModule(new TimeAllocationMutator(controler.getScenario().getConfig()));
		if ( locachoiceWrnCnt < 1 ) {
			locachoiceWrnCnt ++ ;
			Logger.getLogger("dummy").warn("I don't think that using TimeAllocationMutator as last step of locationchoice" +
					" (or of any strategy, for that matter) makes sense. --> please remove from code.   kai, oct'12") ;
			if ( controler.getScenario().getConfig().vspExperimental().getValue(VspExperimentalConfigKey.vspDefaultsCheckingLevel).equals(VspExperimentalConfigGroup.ABORT) ) {
				throw new RuntimeException("will not use locachoice followed by TimeMutation within VSP. Aborting ...") ;
			}
		}
	}

	public LocationChoicePlanStrategy(Config config) {
		
	}
	
	@Override
	public void addStrategyModule(PlanStrategyModule module) {
		delegate.addStrategyModule(module);
	}

	@Override
	public int getNumberOfStrategyModules() {
		return delegate.getNumberOfStrategyModules();
	}

	@Override
	public void run(Person person) {
		delegate.run(person);
	}

	@Override
	public void init() {
		delegate.init();
	}

	@Override
	public void finish() {
		delegate.finish();
	}

	@Override
	public PlanSelector getPlanSelector() {
		return delegate.getPlanSelector();
	}
	
}
