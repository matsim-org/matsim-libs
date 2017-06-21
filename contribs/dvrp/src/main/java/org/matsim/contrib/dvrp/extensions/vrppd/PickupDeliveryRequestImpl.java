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

package org.matsim.contrib.dvrp.extensions.vrppd;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;

public class PickupDeliveryRequestImpl /*extends RequestImpl*/ implements PickupDeliveryRequest {
	private final Link fromLink;
	private final Link toLink;

	private PickupDeliveryTask pickupTask;
	private PickupDeliveryTask deliveryTask;
	
	private RequestImpl delegate ;

	public PickupDeliveryRequestImpl(Id<Request> id, double quantity, double earliestStartTime, double latestStartTime,
			double submissionTime, Link fromLink, Link toLink) {
//		super(id, quantity, earliestStartTime, latestStartTime, submissionTime);
		delegate = new RequestImpl( id, quantity, earliestStartTime, latestStartTime, submissionTime ) ;
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
	public PickupDeliveryTask getPickupTask() {
		return pickupTask;
	}

	public void setPickupTask(PickupDeliveryTask pickupTask) {
		this.pickupTask = pickupTask;
	}

	@Override
	public PickupDeliveryTask getDeliveryTask() {
		return deliveryTask;
	}

	public void setDeliveryTask(PickupDeliveryTask deliveryTask) {
		this.deliveryTask = deliveryTask;
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
