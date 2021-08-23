/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.locationchoice.frozenepsilons;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TripsToLegsModule;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.timing.TimeInterpretation;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

class BestReplyLocationChoicePlanStrategy implements PlanStrategy {

	private PlanStrategyImpl delegate;
	@Inject private Config config ;
	@Inject private Scenario scenario;
	@Inject private Provider<TripRouter> tripRouterProvider;
	@Inject private ScoringFunctionFactory scoringFunctionFactory;
	@Inject private Map<String, TravelTime> travelTimes;
	@Inject private Map<String, TravelDisutilityFactory> travelDisutilities;
	@Inject private TimeInterpretation timeInterpretation;

	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
		delegate.run(person);
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		/*
		 * Somehow this is ugly. Should be initialized in the constructor. But I do not know, how to initialize the lc scenario elements
		 * such that they are already available at the time of constructing this object. ah feb'13
		 */
		FrozenTastesConfigGroup dccg = ConfigUtils.addOrGetModule( config, FrozenTastesConfigGroup.class ) ;

		DestinationChoiceContext lcContext = (DestinationChoiceContext) scenario.getScenarioElement(DestinationChoiceContext.ELEMENT_NAME);

		// only necessary if vou overwritten one of the following classes BestReplyLocationChoicePlanAlgorithm or BestReplyLocationChoiceStrategymodule
		MaxDCScoreWrapper maxDcScoreWrapper = (MaxDCScoreWrapper)scenario.getScenarioElement(MaxDCScoreWrapper.ELEMENT_NAME);

		if ( !FrozenTastesConfigGroup.Algotype.bestResponse.equals(dccg.getAlgorithm() )) {
			throw new RuntimeException("wrong class for selected location choice algorithm type; aborting ...") ;
		}

		switch( dccg.getPlanSelector() ){
			case "BestScore":
				delegate = new PlanStrategyImpl( new BestPlanSelector<>() );
				break;
			case "ChangeExpBeta":
				delegate = new PlanStrategyImpl( new ExpBetaPlanChanger( config.planCalcScore().getBrainExpBeta() ) );
				break;
			case "SelectRandom":
				delegate = new PlanStrategyImpl( new RandomPlanSelector() );
				break;
			default:
				delegate = new PlanStrategyImpl( new ExpBetaPlanSelector( config.planCalcScore() ) );
				break;
		}
		delegate.addStrategyModule(new BestReplyLocationChoiceStrategymodule(tripRouterProvider, lcContext, maxDcScoreWrapper.getPersonsMaxDCScoreUnscaled(), scoringFunctionFactory, travelTimes, travelDisutilities, timeInterpretation) );
		delegate.addStrategyModule(new ReRoute(lcContext.getScenario(), tripRouterProvider, timeInterpretation));
		
		delegate.init(replanningContext);
	}

	@Override
	public void finish() {
		delegate.finish();
	}
}
