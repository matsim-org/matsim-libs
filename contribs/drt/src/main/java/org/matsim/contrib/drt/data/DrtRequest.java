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

package org.matsim.contrib.drt.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

/**
 * @author michalm
 */
public class DrtRequest /*extends RequestImpl*/ implements PassengerRequest {

	public enum DrtRequestStatus {
		UNPLANNED, // submitted by the CUSTOMER and received by the DISPATCHER
		PLANNED, // planned - included into one of the routes
		PICKUP, // being picked up
		RIDE, // on board
		DROPOFF, // being dropped off
		PERFORMED, // completed
		REJECTED; // rejected by the DISPATCHER
	}

	private final MobsimPassengerAgent passenger;
	private final Link fromLink;
	private final Link toLink;
	private DrtStopTask pickupTask = null;
	private DrtStopTask dropoffTask = null;
	private final double latestArrivalTime;
	private final VrpPathWithTravelData unsharedRidePath;
	
	private RequestImpl delegate ;

	public DrtRequest(Id<Request> id, MobsimPassengerAgent passenger, Link fromLink, Link toLink,
			double earliestStartTime, double latestStartTime, double latestArrivalTime, double submissionTime,
			VrpPathWithTravelData unsharedRidePath) {
//		super(id, 1, earliestStartTime, latestStartTime, submissionTime);
		delegate = new RequestImpl( id, 1, earliestStartTime, latestStartTime, submissionTime ) ;
		this.passenger = passenger;
		this.fromLink = fromLink;
		this.toLink = toLink;
		this.latestArrivalTime = latestArrivalTime;
		this.unsharedRidePath = unsharedRidePath;
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

	public double getLatestArrivalTime() {
		return latestArrivalTime;
	}

	public VrpPathWithTravelData getUnsharedRidePath() {
		return unsharedRidePath;
	}

	public DrtRequestStatus getStatus() {
		if (pickupTask == null) {
			return DrtRequestStatus.UNPLANNED;
		}

		switch (pickupTask.getStatus()) {
			case PLANNED:
				return DrtRequestStatus.PLANNED;

			case STARTED:
				return DrtRequestStatus.PICKUP;

			case PERFORMED:// continue
		}

		switch (dropoffTask.getStatus()) {
			case PLANNED:
				return DrtRequestStatus.RIDE;

			case STARTED:
				return DrtRequestStatus.DROPOFF;

			case PERFORMED:
				return DrtRequestStatus.PERFORMED;

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
