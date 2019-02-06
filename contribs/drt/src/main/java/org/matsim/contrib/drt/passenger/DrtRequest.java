/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;

/**
 * @author michalm
 */
public class DrtRequest implements PassengerRequest {
	private final Id<Request> id;
	private final double submissionTime;
	private final double earliestStartTime;
	private final double latestStartTime;
	private final double latestArrivalTime;

	private boolean rejected = false;

	private final Id<Person> passengerId;
	private final String mode;

	private final Link fromLink;
	private final Link toLink;

	private DrtStopTask pickupTask = null;
	private DrtStopTask dropoffTask = null;

	public DrtRequest(Id<Request> id, Id<Person> passengerId, String mode, Link fromLink, Link toLink,
			double earliestStartTime, double latestStartTime, double latestArrivalTime, double submissionTime) {
		this.id = id;
		this.submissionTime = submissionTime;
		this.earliestStartTime = earliestStartTime;
		this.latestStartTime = latestStartTime;
		this.latestArrivalTime = latestArrivalTime;
		this.passengerId = passengerId;
		this.mode = mode;
		this.fromLink = fromLink;
		this.toLink = toLink;
	}

	@Override
	public Id<Request> getId() {
		return id;
	}

	@Override
	public double getSubmissionTime() {
		return submissionTime;
	}

	@Override
	public double getEarliestStartTime() {
		return earliestStartTime;
	}

	@Override
	public double getLatestStartTime() {
		return latestStartTime;
	}

	public double getLatestArrivalTime() {
		return latestArrivalTime;
	}

	@Override
	public boolean isRejected() {
		return rejected;
	}

	@Override
	public void setRejected(boolean rejected) {
		this.rejected = rejected;
	}

	@Override
	public Link getFromLink() {
		return fromLink;
	}

	@Override
	public Link getToLink() {
		return toLink;
	}

	@Override
	public Id<Person> getPassengerId() {
		return passengerId;
	}

	@Override
	public String getMode() {
		return mode;
	}

	public DrtStopTask getPickupTask() {
		return pickupTask;
	}

	public void setPickupTask(DrtStopTask pickupTask) {
		this.pickupTask = pickupTask;
	}

	public DrtStopTask getDropoffTask() {
		return dropoffTask;
	}

	public void setDropoffTask(DrtStopTask dropoffTask) {
		this.dropoffTask = dropoffTask;
	}

	@Override
	public String toString() {
		return Request.toString(this);
	}
}
