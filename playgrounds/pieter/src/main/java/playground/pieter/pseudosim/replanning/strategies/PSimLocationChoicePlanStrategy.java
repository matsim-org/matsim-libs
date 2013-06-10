package playground.pieter.pseudosim.replanning.strategies;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.DestinationChoice;
import org.matsim.contrib.locationchoice.LocationChoicePlanStrategy;
import org.matsim.core.config.groups.LocationChoiceConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import playground.pieter.pseudosim.controler.PseudoSimControler;
import playground.pieter.pseudosim.replanning.modules.PSimPlanMarkerModule;

public class PSimLocationChoicePlanStrategy implements PlanStrategy {

	private PlanStrategyImpl delegate;
	
//	private static int locachoiceWrnCnt;
	
	public PSimLocationChoicePlanStrategy(Scenario scenario, PseudoSimControler controler) {
		if ( LocationChoiceConfigGroup.Algotype.bestResponse==scenario.getConfig().locationchoice().getAlgorithm() ) {
			throw new RuntimeException("best response location choice not supported as part of LocationChoicePlanStrategy. " +
					"Use BestReplyLocationChoicePlanStrategy instead, but be aware that as of now some Java coding is necessary to do that. kai, feb'13") ;
		}
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
		delegate.addStrategyModule(new DestinationChoice(scenario));
		delegate.addStrategyModule(new ReRoute(scenario));
		delegate.addStrategyModule(new PSimPlanMarkerModule(controler));
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
