/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package eu.eunoiaproject.bikesharing.framework.router;

import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.MainModeIdentifier;

import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;

public class BikeSharingModeIdentifier implements MainModeIdentifier {
	private final MainModeIdentifier delegate;

	public BikeSharingModeIdentifier(
			MainModeIdentifier delegate) {
		this.delegate = delegate;
	}

	@Override
	public String identifyMainMode(
			final List<PlanElement> tripElements) {
		boolean hadBikeSharing = false;
		for ( PlanElement pe : tripElements ) {
			if ( pe instanceof Leg ) {
				final Leg l = (Leg) pe;
				if ( l.getMode().equals( BikeSharingConstants.MODE ) ) {
					hadBikeSharing = true;
				}
				if ( l.getMode().equals( TransportMode.transit_walk ) ) {
					return TransportMode.pt;
				}
			}
		}

		if ( hadBikeSharing ) {
			// there were bike sharing legs but no transit walk
			return BikeSharingConstants.MODE;
		}

		return delegate.identifyMainMode( tripElements );
	}
}
