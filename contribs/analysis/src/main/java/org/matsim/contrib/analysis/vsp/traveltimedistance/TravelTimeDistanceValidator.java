/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.analysis.vsp.traveltimedistance;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;

/**
 * @author  jbischoff, Chengqi Lu
 * An Interface for Traveltime Validation
 */

/**
 * 
 */
public interface TravelTimeDistanceValidator {
	/**
	 * 
	 * @param trip the trip to validate
	 * @return a tuple of validated TravelTime and Distance
	 */
	Tuple<Double,Double> getTravelTime(NetworkTrip trip);

	/**
	 * A more general form for the trip validation
	 * @param fromCorrd coordinate of departure location
	 * @param toCoord coordinate of departure location
	 * @param departureTime departure time in seconds during the day
	 * @param tripId id for the trip for detailed record
	 * @return a tuple of validated TravelTime and Distance
	 */
	Tuple<Double, Double> getTravelTime(Coord fromCorrd, Coord toCoord, double departureTime, String tripId);
}
