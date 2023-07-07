
/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultAnalysisMainModeIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Attempt to provide a default {@link AnalysisMainModeIdentifier} that is justified from the transport engineering perspective, not from the
 * software perspective.
 * If you use other transport modes not included here, please copy this class, add the mode in your copy and bind that copy in your run class.
 *
 * @author vsp-gleich
 *
 */
public final class DefaultAnalysisMainModeIdentifier implements AnalysisMainModeIdentifier {
	// Please do not lightheartedly change this class, you are not the only user affected.

	private static final Logger log = LogManager.getLogger(DefaultAnalysisMainModeIdentifier.class);

	private final List<String> modeHierarchy = new ArrayList<>() ;

	public DefaultAnalysisMainModeIdentifier() {
		// If you want to change the mode hierarchy, please create your own copy for that.
		modeHierarchy.add( TransportMode.non_network_walk ) ;
		modeHierarchy.add( "undefined" ) ;
		modeHierarchy.add( TransportMode.transit_walk) ;
		modeHierarchy.add( TransportMode.other ) ;
		modeHierarchy.add( TransportMode.walk ) ;
		modeHierarchy.add( TransportMode.bike ) ;
		modeHierarchy.add( TransportMode.taxi ) ;
		modeHierarchy.add( TransportMode.drt ) ;
		modeHierarchy.add( TransportMode.ride ) ;
		modeHierarchy.add( TransportMode.motorcycle ) ;
		modeHierarchy.add( TransportMode.truck );
		modeHierarchy.add( TransportMode.car ) ;
		modeHierarchy.add( TransportMode.pt ) ;
		modeHierarchy.add( TransportMode.train ) ;
		modeHierarchy.add( TransportMode.ship ) ;
		modeHierarchy.add( TransportMode.airplane ) ;

		modeHierarchy.add( "freight" ) ; // not clear where this should go since it is not passenger traffic, but it is used in many scenarios.

		// NOTE: This hierarchical stuff is not so great: is park-n-ride a car trip or a pt trip?  Could weigh it by distance, or by time spent
		// in respective mode.  Or have combined modes as separate modes.  In any case, can't do it at the leg level, since it does not
		// make sense to have the system calibrate towards something where we have counted the car and the pt part of a multimodal
		// trip as two separate trips. kai, sep'16
	}

	@Override public String identifyMainMode( List<? extends PlanElement> planElements ) {
		int mainModeIndex = -1 ;
		String unknownMode = null;
		for ( PlanElement pe : planElements ) {
			if (pe instanceof Leg leg) {
				int index = modeHierarchy.indexOf( leg.getMode() ) ;
				if ( index < 0 ) {
					/*
					 * The current leg mode is not included in the hierarchy above.
					 *
					 * If besides this unknown mode there are only (access/egress) non_network_walk and walk, we can assume that this unknown mode
					 * is the main mode. That assumption saves creating custom AnalysisMainModeIdentifier if new modes not included in the hierarchy
					 * above are used. However, with multiple modes besides walk and non_network_walk we need to know the hierarchy of those modes
					 * to determine which of those is the main mode.
					 */
					if (unknownMode != null && !unknownMode.equals(leg.getMode())) {
						log.error("Multiple unknown modes in one trip: " + leg.getMode() + " and " + unknownMode + ". The AnalysisMainModeIdentifier" +
							" cannot determine which of those is the main mode because they are both unknown to it. Please bind your own " +
							"AnalysisMainModeIdentifier that interprets all modes used in your scenario correctly.");
						throw new IllegalStateException("unknown modes in AnalysisMainModeIdentifier: " + leg.getMode() + " and " + unknownMode ) ;
					} else {
						unknownMode = leg.getMode();
					}
				}
				if ( index > mainModeIndex ) {
					mainModeIndex = index ;
				}
			}
		}

		if (unknownMode != null) {
			if (mainModeIndex > modeHierarchy.indexOf( TransportMode.walk )) {
				// another mode besides walk and our unknown mode was found before, we don't know which of them is the main mode
				log.error("Unknown mode " + unknownMode + " and " + modeHierarchy.get( mainModeIndex ) + " found in the same trip. The " +
					"AnalysisMainModeIdentifier cannot determine which of those is the main mode because they are both unknown to it. " +
					"Please bind your own AnalysisMainModeIdentifier that interprets all modes used in your scenario correctly.");
				throw new IllegalStateException("unknown mode in AnalysisMainModeIdentifier: " + unknownMode);
			} else {
				return unknownMode;
			}
		}

		return modeHierarchy.get( mainModeIndex ) ;
	}
}
