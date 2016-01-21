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
package playground.thibautd.router.connectionscanalgorithm;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author thibautd
 */
public class Connection implements Comparable<Connection> {
	private final int tripId;
	private final double departureTime, arrivalTime;
	private final Id<TransitStopFacility> departureStation, arrivalStation;

	public Connection(
			final int tripId,
			final double departureTime,
			final double arrivalTime,
			final Id<TransitStopFacility> departureStation,
			final Id<TransitStopFacility> arrivalStation) {
		this.tripId = tripId;
		this.departureTime = departureTime;
		this.arrivalTime = arrivalTime;
		this.departureStation = departureStation;
		this.arrivalStation = arrivalStation;
	}

	public int getTripId() {
		return tripId;
	}

	public double getDepartureTime() {
		return departureTime;
	}

	public double getArrivalTime() {
		return arrivalTime;
	}

	public Id<TransitStopFacility> getDepartureStation() {
		return departureStation;
	}

	public Id<TransitStopFacility> getArrivalStation() {
		return arrivalStation;
	}

	@Override
	public int compareTo(Connection o) {
		return Double.compare( departureTime , o.departureTime );
	}
}
