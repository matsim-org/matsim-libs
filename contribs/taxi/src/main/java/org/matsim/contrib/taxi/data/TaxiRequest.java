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

package org.matsim.contrib.taxi.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.taxi.schedule.TaxiDropoffTask;
import org.matsim.contrib.taxi.schedule.TaxiPickupTask;

/**
 * @author michalm
 */
public class TaxiRequest implements PassengerRequest {
	public enum TaxiRequestStatus {
		UNPLANNED, // submitted by the CUSTOMER and received by the DISPATCHER
		PLANNED, // planned - included into one of the routes
		PICKUP, // being picked up
		RIDE, // on board
		DROPOFF, // being dropped off
		PERFORMED, // completed
		REJECTED; // rejected by the DISPATCHER
	}

	private final Id<Request> id;
	private final double submissionTime;
	private final double earliestStartTime;

	private boolean rejected = false;

	private final Id<Person> passengerId;
	private final String mode;

	private final Link fromLink;
	private final Link toLink;

	private TaxiPickupTask pickupTask;
	private TaxiDropoffTask dropoffTask;

	public TaxiRequest(Id<Request> id, Id<Person> passengerId, String mode, Link fromLink, Link toLink,
			double earliestStartTime, double submissionTime) {
		this.id = id;
		this.submissionTime = submissionTime;
		this.earliestStartTime = earliestStartTime;
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

	@Override
	public boolean isRejected() {
		return rejected;
	}

	@Override
	public void setRejected(boolean rejected) {
		this.rejected = rejected;
	}

	public TaxiPickupTask getPickupTask() {
		return pickupTask;
	}

	public void setPickupTask(TaxiPickupTask pickupTask) {
		this.pickupTask = pickupTask;
	}

	public TaxiDropoffTask getDropoffTask() {
		return dropoffTask;
	}

	public void setDropoffTask(TaxiDropoffTask dropoffTask) {
		this.dropoffTask = dropoffTask;
	}

	public TaxiRequestStatus getStatus() {
		if (pickupTask == null) {
			return TaxiRequestStatus.UNPLANNED;
		}

		switch (pickupTask.getStatus()) {
			case PLANNED:
				return TaxiRequestStatus.PLANNED;

			case STARTED:
				return TaxiRequestStatus.PICKUP;

			case PERFORMED:// continue
		}

		switch (dropoffTask.getStatus()) {
			case PLANNED:
				return TaxiRequestStatus.RIDE;

			case STARTED:
				return TaxiRequestStatus.DROPOFF;

			case PERFORMED:
				return TaxiRequestStatus.PERFORMED;
		}

		throw new IllegalStateException("Unreachable code");
	}

	@Override
	public String toString() {
		return Request.toString(this);
	}
}
