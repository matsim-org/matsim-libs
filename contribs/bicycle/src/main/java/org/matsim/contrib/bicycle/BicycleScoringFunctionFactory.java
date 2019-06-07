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
package org.matsim.contrib.bicycle;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.bicycle.BicycleConfigGroup.BicycleScoringType;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;

import com.google.inject.Inject;

/**
 * @author dziemke
 */
public final class BicycleScoringFunctionFactory implements ScoringFunctionFactory {
	// ok to have this public final when the constructor is package-private/injected: can only used through injection

	@Inject ScoringParametersForPerson parameters;
	
	@Inject Scenario scenario;

	@Inject EventsManager eventsManager;

	/* package */ BicycleScoringFunctionFactory(){}
	
	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		SumScoringFunction sumScoringFunction = new SumScoringFunction();

		final ScoringParameters params = parameters.getScoringParameters(person);
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

		BicycleConfigGroup bicycleConfigGroup = (BicycleConfigGroup) scenario.getConfig().getModules().get(BicycleConfigGroup.GROUP_NAME);
		BicycleScoringType bicycleScoringType = bicycleConfigGroup.getBicycleScoringType();
		if (bicycleScoringType == BicycleScoringType.legBased) {
			sumScoringFunction.addScoringFunction(new BicycleLegScoring(params, scenario.getNetwork(), scenario.getConfig().transit().getTransitModes(), bicycleConfigGroup));
		} else if (bicycleScoringType == BicycleScoringType.linkBased) {
			BicycleLinkScoring bicycleLinkScoring = new BicycleLinkScoring(params, scenario, bicycleConfigGroup);
			sumScoringFunction.addScoringFunction(bicycleLinkScoring);

			CarCounter carCounter = new CarCounter(bicycleLinkScoring);
			eventsManager.addHandler(carCounter);
		} else {
			throw new IllegalArgumentException("Bicycle scoring type " + bicycleScoringType + " not known.");
		}


		return sumScoringFunction;
	}

	
	private class CarCounter implements BasicEventHandler{
		private BicycleLinkScoring bicycleLinkScoring;

		private CarCounter( BicycleLinkScoring bicycleLinkScoring ) {
			this.bicycleLinkScoring = bicycleLinkScoring;
		}

		@Override
		public void handleEvent( Event event ) {
			if ( event instanceof MotorizedInteractionEvent ){
				bicycleLinkScoring.handleEvent( (MotorizedInteractionEvent) event );
			}
		}
	}
}
