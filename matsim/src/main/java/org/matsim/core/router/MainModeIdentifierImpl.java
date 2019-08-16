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

package org.matsim.core.router;

import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;

/**
 * @author thibaut
 */
public final class MainModeIdentifierImpl implements MainModeIdentifier {
	@Override
	public String identifyMainMode( final List<? extends PlanElement> tripElements) {
		String mode = ((Leg) tripElements.get( 0 )).getMode();
//		return mode.equals( TransportMode.transit_walk ) ? TransportMode.pt : mode;
		if ( mode.equals( TransportMode.transit_walk ) ) {
			return TransportMode.pt ;

			// (yy not conforming to std transport planning since that would look for mode with the highest "weight"
			// in the whole trip, but it is what I found and at least one test depends on it. kai, feb'16)

			// Marcel's SBB raptor returns access/egress_walk to and from pt, and transit_walk only
			// for direct walk (and presumably in between pt legs, if necessary).  kai/gregor, sep'18
		}
		
		for ( PlanElement pe : tripElements ) {
			if ( pe instanceof Leg ) {
				Leg leg = (Leg) pe ;
				String mode2 = leg.getMode() ;
				if ( !mode2.contains( TransportMode.non_network_walk ) &&
						!mode2.contains( TransportMode.non_network_walk ) &&
						!mode2.contains( TransportMode.transit_walk ) ) {
					return mode2 ;
				}
			}
		}
		
		/*
		 * For network modes, pt and drt we can encounter the following situation:
		 * If the closest transit stop / link to the origin is also the closest transit stop / link to the destination,
		 * there is no useful route between them, because we would board and get off at the same place.
		 * Previously we had so called direct walks in that case, i.e. a single leg of mode "transit_walk", "drt_walk"
		 * or similar. We want to get rid of these direct walks (see MATSIM-943).
		 * We would then instead obtain non_network_walk --> pt interaction --> non_network_walk.
		 * There is no leg of the main mode, so we try to find out the main mode using the interaction activity. 
		 * In MATSIM-945 we decided to have all stage activities end with "interaction". Assuming an convention such as
		 * stage activities should be called "mode interaction" (currently most or all stage activities follow that 
		 * form) we try to identify the mode  
		 * Something like a router mode would be better for the future. - gleich aug'19
		 */
		if (tripElements.size() == 3 && tripElements.get(0) instanceof Leg && tripElements.get(1) instanceof Activity
				&& tripElements.get(2) instanceof Leg) {
			Leg leg0 = (Leg) tripElements.get(0);
			Activity act = (Activity) tripElements.get(1);
			Leg leg1 = (Leg) tripElements.get(2);

			if (leg0.getMode().equals(TransportMode.non_network_walk) && act.getType().contains("interaction")
					&& leg1.getMode().equals(TransportMode.non_network_walk)) {
				String firstPart = act.getType().split("interaction")[0];
				String mainMode = firstPart.trim();
				return mainMode;
			}
		}

		throw new RuntimeException( "could not identify main mode "+ tripElements) ;
		
	}
}
