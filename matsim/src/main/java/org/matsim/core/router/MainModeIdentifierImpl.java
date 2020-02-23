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
		if ( mode.endsWith( "_fallback" ) )  {
			return mode.substring(0, mode.length() - 9) ;
		}
		
		for ( PlanElement pe : tripElements ) {
			if ( pe instanceof Leg ) {
				Leg leg = (Leg) pe ;
				String mode2 = leg.getMode() ;
				if ( !mode2.contains( TransportMode.non_network_walk ) &&
						!mode2.contains( TransportMode.transit_walk ) ) {
					return mode2 ;
				}
			}
		}

		throw new RuntimeException( "could not identify main mode "+ tripElements) ;
		
	}
}
