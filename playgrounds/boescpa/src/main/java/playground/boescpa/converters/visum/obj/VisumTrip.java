/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.visum.obj;

import playground.boescpa.analysis.trips.Trip;

/**
 * Provides a representation of visum trips.
 *
 * @author boescpa
 */
public class VisumTrip {

	// TODO-boescpa Write tests...

    private final Trip trip;

    public VisumTrip(Trip trip) {
        this.trip = trip;
    }

    public boolean isModeType(String mode) {
		return mode.equals(trip.mode);
	}

	public boolean isWithinZone(Zone interestingArea) {
		boolean withinZone = false;

		// Check startPoint:
		withinZone = interestingArea.isWithinZone(trip.startXCoord, trip.startYCoord);

		// Check endPoint:
		if (!withinZone) {
			withinZone = interestingArea.isWithinZone(trip.endXCoord, trip.endYCoord);
		}

		return withinZone;
	}

	public boolean isWithinHour(int hour) {
		int time = hour*3600;
		boolean withinTime = false;

		// Check startTime:
		withinTime = trip.startTime >= time && trip.startTime <= time + 3600;

		// Check endTime:
		if (!withinTime) {
			withinTime = trip.endTime >= time && trip.endTime <= time + 3600;
		}

		return withinTime;
	}

	public boolean isOriginZone(Zone zone) {
		return zone.isWithinZone(trip.startXCoord, trip.startYCoord);
	}

	public boolean isDestinZone(Zone zone) {
		return zone.isWithinZone(trip.endXCoord, trip.endYCoord);
	}

	public double distanceToCentroid(Zone centroid, boolean origOrDest) {
		// Returns for the current trip the distance to the centroid.
		// origOrDest: If true, then distance to origin, if false, then distance to destination.

		if (origOrDest) {
			// <=> origOrDest = true => Origin
			return centroid.getDistToCentroid(trip.startXCoord, trip.startYCoord);
		}
		else {
			// <=> origOrDest = false => Destination
			return centroid.getDistToCentroid(trip.endXCoord, trip.endYCoord);
		}
	}
}
