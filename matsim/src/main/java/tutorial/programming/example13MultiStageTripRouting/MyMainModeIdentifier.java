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

package tutorial.programming.example13MultiStageTripRouting;

import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.MainModeIdentifier;

/**
 * @author thibautd
 */
public class MyMainModeIdentifier implements MainModeIdentifier {
	private final MainModeIdentifier defaultModeIdentifier;

	public MyMainModeIdentifier(final MainModeIdentifier defaultModeIdentifier) {
		this.defaultModeIdentifier = defaultModeIdentifier;
	}

	@Override
	public String identifyMainMode(List<? extends PlanElement> tripElements) {
		for ( PlanElement pe : tripElements ) {
			if ( pe instanceof Leg && ((Leg) pe).getMode().equals( MyRoutingModule.TELEPORTATION_LEG_MODE ) ) {
				return MyRoutingModule.TELEPORTATION_MAIN_MODE;
			}
		}
		// if the trip doesn't contain a teleportation leg,
		// fall back to the default identification method.
		return defaultModeIdentifier.identifyMainMode( tripElements );
	}
}
