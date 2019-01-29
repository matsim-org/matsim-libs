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

package org.matsim.contrib.socnetsim.jointtrips;

import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.MainModeIdentifier;

import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;

public class JointMainModeIdentifier implements MainModeIdentifier {
	private final MainModeIdentifier d;
	
	public JointMainModeIdentifier( final MainModeIdentifier delegate ) {
		this.d = delegate;
	}

	@Override
	public String identifyMainMode(
			final List<? extends PlanElement> tripElements) {
		for (PlanElement pe : tripElements) {
			if ( !(pe instanceof Leg) ) continue;
			final String mode = ((Leg) pe).getMode();

			if (mode.equals( JointActingTypes.DRIVER ) ||
					mode.equals( JointActingTypes.PASSENGER ) ) {
				return mode;
			}
		}
		return d.identifyMainMode( tripElements );
	}
}
