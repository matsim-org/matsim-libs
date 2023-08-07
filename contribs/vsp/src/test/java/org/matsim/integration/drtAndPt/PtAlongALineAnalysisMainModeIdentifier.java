
/* *********************************************************************** *
 * project: org.matsim.*
 * PtAlongALineAnalysisMainModeIdentifier.java
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

package org.matsim.integration.drtAndPt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.AnalysisMainModeIdentifier;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author vsp-gleich
 *
 */
public final class PtAlongALineAnalysisMainModeIdentifier implements AnalysisMainModeIdentifier {

	private static final Logger log = LogManager.getLogger(PtAlongALineAnalysisMainModeIdentifier.class);

	private final List<String> modeHierarchy = new ArrayList<>() ;

	public PtAlongALineAnalysisMainModeIdentifier() {
		// If you want to change the mode hierarchy, please create your own copy for that.
		modeHierarchy.add( TransportMode.non_network_walk ) ;
		modeHierarchy.add( "undefined" ) ;
		modeHierarchy.add( TransportMode.other ) ;
		modeHierarchy.add( "walk2" ) ;
		modeHierarchy.add( TransportMode.walk ) ;
		modeHierarchy.add( TransportMode.bike ) ;
		modeHierarchy.add( TransportMode.taxi ) ;
		modeHierarchy.add( "drt1" ) ;
		modeHierarchy.add( "drt2" ) ;
		modeHierarchy.add( "drt3" ) ;
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
		for ( PlanElement pe : planElements ) {
			if (pe instanceof Leg leg) {
				int index = modeHierarchy.indexOf( leg.getMode() ) ;
				if ( index < 0 ) {
					log.error("unknown mode=" + leg.getMode() + ". You are using a mode not included in matsim per default. Please bind your own " +
						"AnalysisMainModeIdentifier that interprets all modes used in your scenario correctly.");
					throw new RuntimeException("unknown mode in AnalysisMainModeIdentifier: " + leg.getMode() ) ;
				}
				if ( index > mainModeIndex ) {
					mainModeIndex = index ;
				}
			}
		}
		return modeHierarchy.get( mainModeIndex ) ;
	}
}
