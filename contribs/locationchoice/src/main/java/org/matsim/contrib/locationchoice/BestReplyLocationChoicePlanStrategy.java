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
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.MaxDCScoreWrapper;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.LocationChoiceConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TripsToLegsModule;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

public class BestReplyLocationChoicePlanStrategy implements PlanStrategy {

	private PlanStrategyImpl delegate;
	private Scenario scenario;
	
	public BestReplyLocationChoicePlanStrategy(Scenario scenario) {
		this.scenario = scenario;
	}
		
	@Override
	public void run(HasPlansAndId<Plan> person) {
		delegate.run(person);
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		/*
		 * Somehow this is ugly. Should be initialized in the constructor. But I do not know, how to initialize the lc scenario elements
		 * such that they are already available at the time of constructing this object. ah feb'13
		 */
		DestinationChoiceBestResponseContext lcContext = (DestinationChoiceBestResponseContext) scenario.getScenarioElement(DestinationChoiceBestResponseContext.ELEMENT_NAME);
		MaxDCScoreWrapper maxDcScoreWrapper = (MaxDCScoreWrapper)scenario.getScenarioElement(MaxDCScoreWrapper.ELEMENT_NAME);
		if ( !LocationChoiceConfigGroup.Algotype.bestResponse.equals(lcContext.getScenario().getConfig().locationchoice().getAlgorithm())) {
			throw new RuntimeException("wrong class for selected location choice algorithm type; aborting ...") ;
		}		
		Config config = lcContext.getScenario().getConfig() ;
		String planSelector = config.locationchoice().getPlanSelector();
		if (planSelector.equals("BestScore")) {
			delegate = new PlanStrategyImpl(new BestPlanSelector<Plan>());
		} else if (planSelector.equals("ChangeExpBeta")) {
			delegate = new PlanStrategyImpl(new ExpBetaPlanChanger(config.planCalcScore().getBrainExpBeta()));
		} else if (planSelector.equals("SelectRandom")) {
			delegate = new PlanStrategyImpl(new RandomPlanSelector());
		} else {
			delegate = new PlanStrategyImpl(new ExpBetaPlanSelector(config.planCalcScore()));
		}
		delegate.addStrategyModule(new TripsToLegsModule(scenario.getConfig()));
		delegate.addStrategyModule(new BestReplyDestinationChoice(lcContext, maxDcScoreWrapper.getPersonsMaxDCScoreUnscaled()));
		delegate.addStrategyModule(new ReRoute(lcContext.getScenario()));
		
		delegate.init(replanningContext);
	}

	@Override
	public void finish() {
		delegate.finish();
	}
}
