/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import org.matsim.core.controler.AbstractModule;

/**
 * @author smetzler, dziemke
 */
public final class BicycleModule extends AbstractModule {
	private boolean considerMotorizedInteraction;
	
	@Override
	public void install() {
		bind(BicycleTravelTime.class).asEagerSingleton();
		addTravelTimeBinding("bicycle").to(BicycleTravelTime.class);
		bind(BicycleTravelDisutilityFactory.class).asEagerSingleton();
		addTravelDisutilityFactoryBinding("bicycle").to(BicycleTravelDisutilityFactory.class);
		bindScoringFunctionFactory().toInstance(new BicycleScoringFunctionFactory());
		// The following leads to "Tried proxying org.matsim.core.scoring.ScoringFunctionsForPopulation to support a circular dependency, but it is not an interface."
//		bindScoringFunctionFactory().to(BicycleScoringFunctionFactory.class);
		
		if (considerMotorizedInteraction) {
			addMobsimListenerBinding().to(MotorizedInteractionEngine.class);
		}
	}
	
	public void setConsiderMotorizedInteraction(boolean considerMotorizedInteraction) {
		this.considerMotorizedInteraction = considerMotorizedInteraction;
	}
}
