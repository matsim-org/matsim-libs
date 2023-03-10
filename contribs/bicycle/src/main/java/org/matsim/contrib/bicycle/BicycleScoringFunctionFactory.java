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

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.bicycle.BicycleConfigGroup.BicycleScoringType;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;

/**
 * @author dziemke
 */
/**
* @deprecated -- The {@link BicycleScoringType#linkBased} is already running through {@link BicycleScoreEventsCreator}; for {@link
* BicycleScoringType#legBased} the same should be done.  However, the {@link MotorizedInteractionEngine} is also not implemented in a way that it will
* actually work.
 */
final class BicycleScoringFunctionFactory implements ScoringFunctionFactory {
	// ok to have this public final when the constructor is package-private/injected: can only used through injection

	@Inject
	private ScoringParametersForPerson parameters;

	@Inject
	private Scenario scenario;

	@Inject
	private EventsManager eventsManager;

	@Inject
	private BicycleConfigGroup bicycleConfigGroup;

	@Inject
	private BicycleScoringFunctionFactory() {
	}
	
	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		SumScoringFunction sumScoringFunction = new SumScoringFunction();

		final ScoringParameters params = parameters.getScoringParameters(person);
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring( params ));

		BicycleScoringType bicycleScoringType = bicycleConfigGroup.getBicycleScoringType();

		if (bicycleScoringType == BicycleScoringType.legBased) {
			throw new RuntimeException( "this execution path should no longer be used.");
//			sumScoringFunction.addScoringFunction(new BicycleLegScoring(params, scenario.getNetwork(), scenario.getConfig().transit().getTransitModes(), bicycleConfigGroup));
		} else if (bicycleScoringType == BicycleScoringType.linkBased) {
			BicycleLinkScoring bicycleLinkScoring = new BicycleLinkScoring(params, scenario, bicycleConfigGroup);
			sumScoringFunction.addScoringFunction(bicycleLinkScoring);
			
			CarCounter carCounter = new CarCounter( bicycleLinkScoring );
			eventsManager.addHandler(carCounter);
		} else {
			throw new IllegalArgumentException("Bicycle scoring type " + bicycleScoringType + " not known.");
		}

		return sumScoringFunction;
	}

	
	private static class CarCounter implements BasicEventHandler{
		private final BicycleLinkScoring bicycleLinkScoring;

		private CarCounter( BicycleLinkScoring bicycleLinkScoring ) {
			this.bicycleLinkScoring = bicycleLinkScoring;
		}

		@Override
		public void handleEvent( Event event ) {
			if ( event instanceof MotorizedInteractionEvent ){
				bicycleLinkScoring.handleEvent(event);
			}
		}
	}
}
