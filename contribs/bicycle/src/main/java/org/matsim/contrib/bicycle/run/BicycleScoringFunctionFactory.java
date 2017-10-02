/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * ScoringFunctionFactory.java                                             *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.bicycle.MotorizedInteractionEvent;
import org.matsim.contrib.bicycle.MotorizedInteractionEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.ScoringFunctionsForPopulation;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;

import com.google.inject.Inject;

/**
 * @author dziemke
 */
public class BicycleScoringFunctionFactory implements ScoringFunctionFactory {
	@Inject ScoringParametersForPerson parameters;

	@Inject ScoringFunctionsForPopulation scoringFunctionsForPopulation;
	
	
	@Inject Scenario scenario;
	@Inject BicycleTravelTime bicycleTravelTime;
	@Inject BicycleTravelDisutilityFactory bicycleTravelDisutilityFactory;
	
	@Inject EventsManager eventsManager;


	@Override
	public ScoringFunction createNewScoringFunction(Person person) {		
		SumScoringFunction sumScoringFunction = new SumScoringFunction();

		final ScoringParameters params = parameters.getScoringParameters(person);
		// No leg scoring here as it would only double-count
		// sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, scenario.getNetwork()));
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

		scoringFunctionsForPopulation.setPassLinkEventsToPerson(true);
		// yyyyyy this is the ONLY place where we need to make ScoringFunctionsForPopulation public.  Somehow,
		// this would imply to me that we should attach this switch to something else. kai, sep'17
		
		BicycleScoring bicycleScoring = new BicycleScoring(scenario, bicycleTravelTime, bicycleTravelDisutilityFactory);
		sumScoringFunction.addScoringFunction(bicycleScoring);
		
		CarCounter carCounter = new CarCounter(bicycleScoring);
		eventsManager.addHandler(carCounter);

		return sumScoringFunction;
	}
	
	private class CarCounter implements MotorizedInteractionEventHandler {
		private BicycleScoring bicycleScoring;

		public CarCounter(BicycleScoring bicycleScoring) {
			this.bicycleScoring = bicycleScoring;
		}

		@Override
		public void handleEvent(MotorizedInteractionEvent event) {
			bicycleScoring.handleEvent(event);
		}
	}
}