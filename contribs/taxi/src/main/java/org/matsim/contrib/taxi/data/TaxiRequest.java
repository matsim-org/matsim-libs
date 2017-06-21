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
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.taxi.schedule.*;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

/**
 * @author michalm
 */
public class TaxiRequest /*extends RequestImpl*/ implements PassengerRequest {
	public enum TaxiRequestStatus {
		// INACTIVE, // invisible to the dispatcher (ARTIFICIAL STATE!)
		UNPLANNED, // submitted by the CUSTOMER and received by the DISPATCHER
		PLANNED, // planned - included into one of the routes

		// we have to carry out the request
		PICKUP, RIDE, DROPOFF,

		PERFORMED, //
		// REJECTED, // rejected by the DISPATCHER
		// CANCELLED, // canceled by the CUSTOMER
		;
	}

	private final MobsimPassengerAgent passenger;
	private final Link fromLink;
	private Link toLink;// toLink may be provided during the pickup

	private TaxiPickupTask pickupTask;
	private TaxiDropoffTask dropoffTask;
	
	private RequestImpl delegate ;

	public TaxiRequest(Id<Request> id, MobsimPassengerAgent passenger, Link fromLink, double startTime,
			double submissionTime) {
		this(id, passenger, fromLink, null, startTime, submissionTime);
	}

	public TaxiRequest(Id<Request> id, MobsimPassengerAgent passenger, Link fromLink, Link toLink, double startTime,
			double submissionTime) {
//		super(id, 1, startTime, startTime, submissionTime);
		delegate = new RequestImpl( id, 1, startTime, startTime, submissionTime ) ;
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

	public void setToLink(Link toLink) {
		this.toLink = toLink;
	}

	@Override
	public MobsimPassengerAgent getPassenger() {
		return passenger;
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

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return delegate.hashCode();
	}

	/**
	 * @return
	 * @see org.matsim.contrib.dvrp.data.RequestImpl#getId()
	 */
	public Id<Request> getId() {
		return delegate.getId();
	}

	/**
	 * @return
	 * @see org.matsim.contrib.dvrp.data.RequestImpl#getQuantity()
	 */
	public double getQuantity() {
		return delegate.getQuantity();
	}

	/**
	 * @return
	 * @see org.matsim.contrib.dvrp.data.RequestImpl#getEarliestStartTime()
	 */
	public double getEarliestStartTime() {
		return delegate.getEarliestStartTime();
	}

	/**
	 * @return
	 * @see org.matsim.contrib.dvrp.data.RequestImpl#getLatestStartTime()
	 */
	public double getLatestStartTime() {
		return delegate.getLatestStartTime();
	}

	/**
	 * @return
	 * @see org.matsim.contrib.dvrp.data.RequestImpl#getSubmissionTime()
	 */
	public double getSubmissionTime() {
		return delegate.getSubmissionTime();
	}

	/**
	 * @return
	 * @see org.matsim.contrib.dvrp.data.RequestImpl#isRejected()
	 */
	public boolean isRejected() {
		return delegate.isRejected();
	}

	/**
	 * @param rejected
	 * @see org.matsim.contrib.dvrp.data.RequestImpl#setRejected(boolean)
	 */
	public void setRejected(boolean rejected) {
		delegate.setRejected(rejected);
	}

	/**
	 * @return
	 * @see org.matsim.contrib.dvrp.data.RequestImpl#toString()
	 */
	public String toString() {
		return delegate.toString();
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}
}
