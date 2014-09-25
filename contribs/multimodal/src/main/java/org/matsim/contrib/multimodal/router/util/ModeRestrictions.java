/* *********************************************************************** *
 * project: org.matsim.*
 * ModeRestrictions.java
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

package org.matsim.contrib.multimodal.router.util;

import org.matsim.api.core.v01.TransportMode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class ModeRestrictions {

	/*
	 * Define restrictions for the supported modes.
	 */
	public Map<String, Set<String>> initModeRestrictions() {
		
		Map<String, Set<String>> modeRestrictions = new HashMap<>();
		
		/*
		 * Car
		 */	
		Set<String> carModeRestrictions = new HashSet<>();
		carModeRestrictions.add(TransportMode.car);
		modeRestrictions.put(TransportMode.car, carModeRestrictions);
		
		/*
		 * Walk
		 */	
		Set<String> walkModeRestrictions = new HashSet<>();
		walkModeRestrictions.add(TransportMode.bike);
		walkModeRestrictions.add(TransportMode.walk);
		modeRestrictions.put(TransportMode.walk, walkModeRestrictions);
				
		/*
		 * Bike
		 * Besides bike mode we also allow walk mode - but then the
		 * agent only travels with walk speed (handled in MultiModalTravelTimeCost).
		 */
		Set<String> bikeModeRestrictions = new HashSet<>();
		bikeModeRestrictions.add(TransportMode.walk);
		bikeModeRestrictions.add(TransportMode.bike);
		modeRestrictions.put(TransportMode.bike, bikeModeRestrictions);
		
		/*
		 * PT
		 * We assume PT trips are possible on every road that can be used by cars.
		 * 
		 * Additionally we also allow pt trips to use walk and / or bike only links.
		 * On those links the traveltimes are quite high and we can assume that they
		 * are only use e.g. to walk from the origin to the bus station or from the
		 * bus station to the destination.
		 */
		Set<String> ptModeRestrictions = new HashSet<>();
		ptModeRestrictions.add(TransportMode.pt);
		ptModeRestrictions.add(TransportMode.car);
		ptModeRestrictions.add(TransportMode.bike);
		ptModeRestrictions.add(TransportMode.walk);
		modeRestrictions.put(TransportMode.pt, ptModeRestrictions);
		
		/*
		 * Ride
		 * We assume ride trips are possible on every road that can be used by cars.
		 * Additionally we also allow ride trips to use walk and / or bike only links.
		 * For those links walk travel times are used.
		 */
		Set<String> rideModeRestrictions = new HashSet<>();
		rideModeRestrictions.add(TransportMode.car);
		rideModeRestrictions.add(TransportMode.bike);
		rideModeRestrictions.add(TransportMode.walk);
		modeRestrictions.put(TransportMode.ride, rideModeRestrictions);
		
		return modeRestrictions;
	}
}
