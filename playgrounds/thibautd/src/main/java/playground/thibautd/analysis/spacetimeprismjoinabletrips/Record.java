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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import playground.thibautd.analysis.joinabletripsidentifier.Trip;

/**
 * @author thibautd
 */
public class Record {
	private final Id<Trip> tripId;
	private final int tripNr;
	private final Id<Person> agentId;
	private final Id<Link> originLink, destinationLink;
	private final double departureTime, arrivalTime;
	private final String tripMode;

	public Record(
			final Id<Trip> tripId,
			final int tripNr,
			final String mode,
			final Id<Person> agentId,
			final Id<Link> originLink,
			final Id<Link> destinationLink,
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

	// /////////////////////////////////////////////////////////////////////////
	// getters
	// /////////////////////////////////////////////////////////////////////////
	public int getNumberOfTripInAgentPlan() {
		return this.tripNr;
	}

	public Id<Trip> getTripId() {
		return this.tripId;
	}

	public Id<Person> getAgentId() {
		return this.agentId;
	}

	public Id<Link> getOriginLink() {
		return this.originLink;
	}

	public Id<Link> getDestinationLink() {
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

	// /////////////////////////////////////////////////////////////////////////
	// package private getters/setters: hack to reduce the number of table
	// lookups in the network
	// /////////////////////////////////////////////////////////////////////////
	private Link originLinkRef = null;
	private Link destinationLinkRef = null;

	Link getOriginLinkRef() {
		return originLinkRef;
	}

	Link getDestinationLinkRef() {
		return destinationLinkRef;
	}

	void setOriginLinkRef(final Link l) {
		this.originLinkRef = l;
	}

	void setDestinationLinkRef(final Link l) {
		this.destinationLinkRef = l;
	}

	private double estimatedNetworkDistance = -1;
	private double estimatedNetworkDuration = -1;

	double getEstimatedNetworkDuration() {
		return estimatedNetworkDuration;
	}

	void setEstimatedNetworkDuration(final double estimatedNetworkDuration) {
		this.estimatedNetworkDuration = estimatedNetworkDuration;
	}

	double getEstimatedNetworkDistance() {
		return estimatedNetworkDistance;
	}

	void setEstimatedNetworkDistance(final double estimatedNetworkDistance) {
		this.estimatedNetworkDistance = estimatedNetworkDistance;
	}

	// /////////////////////////////////////////////////////////////////////////
	// equals
	// /////////////////////////////////////////////////////////////////////////
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
