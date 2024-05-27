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
package org.matsim.core.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

/**
 * Intention of this class is to have the controler defaults clearly marked and visible.
 * <p></p>
 * The initial use case of this is that we want to deprecated the pattern of taking out a factory from the controler, wrapping something
 * around it, and putting it back in, since such things may depend on the calling sequence and may thus be unstable.  Users
 * should then rather use the default factory (provided here) and wrap everything around it in a sequence they control themselves.
 * <p></p>
 * I just renamed this from ControlerUtils to ControlerDefaults since XxxUtils is for us, in many case, the outmost user interface,
 * and the material here IMO is not "outermost".  kai, nov'13
 *
 * @author nagel
 *
 * @deprecated -- this pre-dates guice injection; one should rather use guice and {@link ControlerDefaultsModule}.  kai, mar'20
 */
public final class ControlerDefaults {

	private ControlerDefaults(){} // should not be instantiated

	/**
	 * @deprecated -- this pre-dates guice injection; one should rather use guice and {@link ControlerDefaultsModule}.  kai, mar'20
	 */
	public static ScoringFunctionFactory createDefaultScoringFunctionFactory(Scenario scenario) {
		return new CharyparNagelScoringFunctionFactory( scenario );
	}

	/**
	 * @deprecated -- this pre-dates guice injection; one should rather use guice and {@link ControlerDefaultsModule}.  kai, mar'20
	 */
	public static TravelDisutilityFactory createDefaultTravelDisutilityFactory(Scenario scenario) {
		final RandomizingTimeDistanceTravelDisutilityFactory builder = new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car, scenario.getConfig() );
                // tendency to set this to 3. right away (i.e. through PlansCalcRouteConfigGroup default). kai/bk, mar'15
                return builder;
	}

}
