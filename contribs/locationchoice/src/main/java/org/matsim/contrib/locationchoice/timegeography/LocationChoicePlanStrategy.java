package org.matsim.contrib.locationchoice.timegeography;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;

import javax.inject.Provider;

public class LocationChoicePlanStrategy implements PlanStrategy {

	private PlanStrategyImpl delegate;
	
	public LocationChoicePlanStrategy(Scenario scenario, Provider<TripRouter> tripRouterProvider) {
		final DestinationChoiceConfigGroup destinationChoiceConfigGroup = ConfigUtils.addOrGetModule( scenario.getConfig(), DestinationChoiceConfigGroup.class ) ;
		if ( DestinationChoiceConfigGroup.Algotype.bestResponse== destinationChoiceConfigGroup.getAlgorithm() ) {
			throw new RuntimeException("best response location choice not supported as part of LocationChoicePlanStrategy. " +
					"Use BestReplyLocationChoicePlanStrategy instead, but be aware that as of now some Java coding is necessary to do that. kai, feb'13") ;
		}
		switch( destinationChoiceConfigGroup.getPlanSelector() ){
			case "BestScore":
				delegate = new PlanStrategyImpl( new BestPlanSelector() );
				break;
			case "ChangeExpBeta":
				delegate = new PlanStrategyImpl( new ExpBetaPlanChanger( scenario.getConfig().planCalcScore().getBrainExpBeta() ) );
				break;
			case "SelectRandom":
				delegate = new PlanStrategyImpl( new RandomPlanSelector() );
				break;
			default:
				delegate = new PlanStrategyImpl( new ExpBetaPlanSelector( scenario.getConfig().planCalcScore() ) );
				break;
		}
		delegate.addStrategyModule(new DestinationChoice( tripRouterProvider, scenario) );
		delegate.addStrategyModule(new ReRoute(scenario, tripRouterProvider));
	}
	
	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
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
