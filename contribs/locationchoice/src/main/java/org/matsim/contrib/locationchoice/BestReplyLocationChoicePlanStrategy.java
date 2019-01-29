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

package org.matsim.contrib.locationchoice;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceContext;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.MaxDCScoreWrapper;
import org.matsim.core.config.Config;
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

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

public class BestReplyLocationChoicePlanStrategy implements PlanStrategy {

	private PlanStrategyImpl delegate;
	private Scenario scenario;
	private final Provider<TripRouter> tripRouterProvider;
	private ScoringFunctionFactory scoringFunctionFactory;
	private Map<String, TravelTime> travelTimes;
	private Map<String, TravelDisutilityFactory> travelDisutilities;

	@Inject
	BestReplyLocationChoicePlanStrategy(Scenario scenario, Provider<TripRouter> tripRouterProvider, ScoringFunctionFactory scoringFunctionFactory, Map<String, TravelTime> travelTimes, Map<String, TravelDisutilityFactory> travelDisutilities) {
		this.scenario = scenario;
		this.tripRouterProvider = tripRouterProvider;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.travelTimes = travelTimes;
		this.travelDisutilities = travelDisutilities;
	}
		
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
		DestinationChoiceContext lcContext = (DestinationChoiceContext) scenario.getScenarioElement(DestinationChoiceContext.ELEMENT_NAME);
		Config config = lcContext.getScenario().getConfig();
		DestinationChoiceConfigGroup dccg = (DestinationChoiceConfigGroup) config.getModule(DestinationChoiceConfigGroup.GROUP_NAME);
		MaxDCScoreWrapper maxDcScoreWrapper = (MaxDCScoreWrapper)scenario.getScenarioElement(MaxDCScoreWrapper.ELEMENT_NAME);
		if ( !DestinationChoiceConfigGroup.Algotype.bestResponse.equals(dccg.getAlgorithm())) {
			throw new RuntimeException("wrong class for selected location choice algorithm type; aborting ...") ;
		}		
		String planSelector = dccg.getPlanSelector();
		if (planSelector.equals("BestScore")) {
			delegate = new PlanStrategyImpl(new BestPlanSelector<Plan, Person>());
		} else if (planSelector.equals("ChangeExpBeta")) {
			delegate = new PlanStrategyImpl(new ExpBetaPlanChanger(config.planCalcScore().getBrainExpBeta()));
		} else if (planSelector.equals("SelectRandom")) {
			delegate = new PlanStrategyImpl(new RandomPlanSelector());
		} else {
			delegate = new PlanStrategyImpl(new ExpBetaPlanSelector(config.planCalcScore()));
		}
		delegate.addStrategyModule(new TripsToLegsModule(tripRouterProvider, config.global()));
		delegate.addStrategyModule(new BestReplyDestinationChoice(tripRouterProvider, lcContext, maxDcScoreWrapper.getPersonsMaxDCScoreUnscaled(), scoringFunctionFactory, travelTimes, travelDisutilities));
		delegate.addStrategyModule(new ReRoute(lcContext.getScenario(), tripRouterProvider));
		
		delegate.init(replanningContext);
	}

	@Override
	public void finish() {
		delegate.finish();
	}
}
