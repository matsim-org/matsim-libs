package org.matsim.contrib.locationchoice.frozenepsilons;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;

public class FrozenTastes{
	public static final String LOCATION_CHOICE_PLAN_STRATEGY = "frozenTastesLocationChoicePlanStrategy" ;

	private FrozenTastes(){} // do not instantiate


	public static void configure( Controler controler ){

		FrozenTastesConfigGroup ftConfig = ConfigUtils.addOrGetModule( controler.getConfig(), FrozenTastesConfigGroup.class );;

		Scenario scenario = controler.getScenario() ;

		final DestinationChoiceContext lcContext = new DestinationChoiceContext(scenario) ;
		scenario.addScenarioElement(DestinationChoiceContext.ELEMENT_NAME, lcContext);

		DCScoringFunctionFactory scoringFunctionFactory = new DCScoringFunctionFactory(scenario, lcContext);
		scoringFunctionFactory.setUsingConfigParamsForScoring( ftConfig.getUseConfigParamsForScoring() ) ;
		controler.setScoringFunctionFactory(scoringFunctionFactory);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding( LOCATION_CHOICE_PLAN_STRATEGY ).to( BestReplyLocationChoicePlanStrategy.class );
			}
		});
	}
}
