/* *********************************************************************** *
 * project: org.matsim.*
 * RideTravelTime.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.multimodalsimengine.router.util;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * 
 * @author cdobler
 */
public class RideTravelTime implements PersonalizableTravelTime {

	private final TravelTime carTravelTime;	// ride speed does not depend on a passenger, therefore not personalizable
	private final TravelTime walkTravelTime;
	
	public RideTravelTime(TravelTime carTravelTime, TravelTime walkTravelTime) {
		this.carTravelTime = carTravelTime;
		this.walkTravelTime = walkTravelTime;
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		/*
		 * If it is a car link, we use car travel times. Else we check whether it is
		 * a bike / walk link - if it is one, we use walk travel times (the passenger
		 * might be walking to the meeting point).
		 */
		if (link.getAllowedModes().contains(TransportMode.car)) {
			if (carTravelTime instanceof BufferedTravelTime) return ((BufferedTravelTime) carTravelTime).getBufferedLinkTravelTime(link, time);
			else return carTravelTime.getLinkTravelTime(link, time, person, vehicle);
		}
		else if (link.getAllowedModes().contains(TransportMode.bike) ||link.getAllowedModes().contains(TransportMode.walk)) {
			return walkTravelTime.getLinkTravelTime(link, time, person, vehicle);
		}
		
		return link.getLength() / 1.0;
	}


}