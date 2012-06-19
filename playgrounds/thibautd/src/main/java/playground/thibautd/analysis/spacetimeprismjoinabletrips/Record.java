/* *********************************************************************** *
 * project: org.matsim.*
 * Record.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.thibautd.analysis.spacetimeprismjoinabletrips;

import org.matsim.api.core.v01.Id;

/**
 * @author thibautd
 */
public class Record {
	private final Id tripId;
	private final int tripNr;
	private final Id agentId;
	private final Id originLink, destinationLink;
	private final double departureTime, arrivalTime;
	private final String tripMode;

	public Record(
			final Id tripId,
			final int tripNr,
			final String mode,
			final Id agentId,
			final Id originLink,
			final Id destinationLink,
			final double departureTime,
			final double arrivalTime) {
		this.tripId = tripId;
		this.tripMode = mode;
		this.tripNr = tripNr; 
		this.agentId= agentId;
		this.originLink = originLink;
		this.destinationLink = destinationLink;
		this.departureTime = departureTime;
		this.arrivalTime = arrivalTime;
	}

	public int getNumberOfTripInAgentPlan() {
		return this.tripNr;
	}

	public Id getTripId() {
		return this.tripId;
	}

	public Id getAgentId() {
		return this.agentId;
	}

	public Id getOriginLink() {
		return this.originLink;
	}

	public Id getDestinationLink() {
		return this.destinationLink;
	}

	public double getDepartureTime() {
		return this.departureTime;
	}

	public double getArrivalTime() {
		return this.arrivalTime;
	}

	/**
	 * Gets the tripMode for this instance.
	 *
	 * @return The tripMode.
	 */
	public String getTripMode() {
		return this.tripMode;
	}

	@Override
	public boolean equals(final Object other) {
		return other instanceof Record &&
			((Record) other).originLink.equals( originLink ) &&
			((Record) other).destinationLink.equals( destinationLink ) &&
			((Record) other).departureTime == departureTime &&
			((Record) other).arrivalTime == arrivalTime;
	}

	@Override
	public int hashCode() {
		return tripNr +
			agentId.hashCode() +
			originLink.hashCode() +
			destinationLink.hashCode() +
			(int) departureTime + 
			(int) arrivalTime +
			tripMode.hashCode();
	}
}
