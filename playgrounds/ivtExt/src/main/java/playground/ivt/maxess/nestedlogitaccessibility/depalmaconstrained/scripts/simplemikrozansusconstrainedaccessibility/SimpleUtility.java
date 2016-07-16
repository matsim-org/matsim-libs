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
package playground.ivt.maxess.nestedlogitaccessibility.depalmaconstrained.scripts.simplemikrozansusconstrainedaccessibility;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.ivt.maxess.nestedlogitaccessibility.depalmaconstrained.SingleNest;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Alternative;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Utility;

import java.util.List;

/**
 * @author thibautd
 */
public class SimpleUtility implements Utility<SingleNest> {
	private final UtilityConfigGroup configGroup;
	private final ObjectAttributes personAttributes;
	private final MainModeIdentifier modeIdentifier;

	public SimpleUtility(
			final UtilityConfigGroup configGroup,
			final ObjectAttributes personAttributes,
			final MainModeIdentifier modeIdentifier ) {
		this.configGroup = configGroup;
		this.personAttributes = personAttributes;
		this.modeIdentifier = modeIdentifier;
	}

	@Override
	public double calcUtility( final Person p, final Alternative<SingleNest> a ) {
		final double travelTime = getTravelTime( a );
		final String mode = modeIdentifier.identifyMainMode( a.getAlternative().getTrip() );

		switch ( mode ) {
			case "car":
				return configGroup.getBetaTtCar() * travelTime;
			case "pt":
				return configGroup.getBetaTtPt() * travelTime;
			default:
				throw new IllegalArgumentException( mode );
		}
	}

	private double getTravelTime( Alternative<SingleNest> a ) {
		final List<? extends PlanElement> trip = a.getAlternative().getTrip();

		double tt = 0;

		for ( PlanElement pe : trip ) {
			if ( pe instanceof Leg )  {
				tt += ( (Leg) pe ).getTravelTime();
			}
		}

		assert tt >= 0 : tt;

		switch ( configGroup.getFunctionalForm() ) {
			case linear:
				return tt;
			case log:
				return Math.log( 1 + tt );
			default:
				throw new IllegalArgumentException( configGroup.getFunctionalForm()+"?" );
		}
	}
}
