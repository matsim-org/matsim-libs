/* *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** */

package org.matsim.contrib.taxibus;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.taxibus.tasks.*;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

/**
 * @author jbischoff (might not be needed)
 */
public class TaxibusRequest extends RequestImpl implements PassengerRequest {

	public enum TaxibusRequestStatus {
		// INACTIVE, // invisible to the dispatcher (ARTIFICIAL STATE!)
		UNPLANNED, // submitted by the CUSTOMER and received by the DISPATCHER
		PLANNED, // planned - included into one of the routes

		// we have to carry out the request
		// - difference between taxi and taxibus seems to minimal, but the actual tasks are different, because
		// Pickuptasks for certain requests may be with customers on board already
		PICKUP, RIDE, DROPOFF,

		PERFORMED, //
		// REJECTED, // rejected by the DISPATCHER
		// CANCELLED, // canceled by the CUSTOMER
		;
	}

	private final MobsimPassengerAgent passenger;
	private final Link fromLink;
	private final Link toLink;
	private TaxibusPickupTask pickupTask = null;
	private TaxibusDropoffTask dropoffTask = null;;

	public TaxibusRequest(Id<Request> id, MobsimPassengerAgent passenger, Link fromLink, Link toLink,
			double earliestStartTime, double latestStartTime, double submissionTime) {
		super(id, 1, earliestStartTime, latestStartTime, submissionTime);
		this.passenger = passenger;
		this.fromLink = fromLink;
		this.toLink = toLink;
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
	public MobsimPassengerAgent getPassenger() {
		return passenger;
	}

	public TaxibusPickupTask getPickupTask() {
		return pickupTask;
	}

	public void setPickupTask(TaxibusPickupTask pickupTask) {
		this.pickupTask = pickupTask;
	}

	public TaxibusDropoffTask getDropoffTask() {
		return dropoffTask;
	}

	public void setDropoffTask(TaxibusDropoffTask dropoffTask) {
		this.dropoffTask = dropoffTask;
	}

	public TaxibusRequestStatus getStatus() {
		if (pickupTask == null) {
			return TaxibusRequestStatus.UNPLANNED;
		}

		switch (pickupTask.getStatus()) {
			case PLANNED:
				return TaxibusRequestStatus.PLANNED;

			case STARTED:
				return TaxibusRequestStatus.PICKUP;

			case PERFORMED:// continue
		}

		switch (dropoffTask.getStatus()) {
			case PLANNED:
				return TaxibusRequestStatus.RIDE;

			case STARTED:
				return TaxibusRequestStatus.DROPOFF;

			case PERFORMED:
				return TaxibusRequestStatus.PERFORMED;

		}

		throw new IllegalStateException("Unreachable code");
	}
}
