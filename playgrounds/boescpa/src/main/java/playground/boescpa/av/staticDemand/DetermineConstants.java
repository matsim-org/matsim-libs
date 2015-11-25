/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.av.staticDemand;

import org.matsim.core.utils.geometry.CoordUtils;
import playground.boescpa.analysis.trips.tripReader.Trip;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class DetermineConstants {

	public static void main(String[] args) {
		// Beeline factor street:
		final Map<Long, Trip> tripsOriginal = Trip.createTripCollection(args[0]);
		final List<Trip> filteredTrips = filterTrips(tripsOriginal);
		double totalStreetDist = 0;
		double totalBeelineDist = 0;
		for (Trip trip : filteredTrips) {
			totalStreetDist += trip.distance;
			totalBeelineDist += CoordUtils.calcDistance(
                    CoordUtils.createCoord(trip.startXCoord, trip.startYCoord),
                    CoordUtils.createCoord(trip.endXCoord, trip.endYCoord));
		}
		System.out.println("Beeline Factor Street = " + (totalStreetDist / totalBeelineDist));
	}

	private static List<Trip> filterTrips(Map<Long, Trip> trips) {
		List<Trip> modeFilteredTrips = new ArrayList<>();
		// filter mode:
		for (String mode : Constants.MODES_REPLACED_BY_AV) {
			for (Trip trip : trips.values()) {
				if (mode.equals(trip.mode)) {
					modeFilteredTrips.add(trip);
				}
			}
		}
		return modeFilteredTrips;
	}
}
