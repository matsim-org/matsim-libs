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

package org.matsim.contrib.dvrp.examples.onetaxi;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

/**
 * @author michalm
 */
public final class OneTaxiRequest /*extends RequestImpl*/ implements PassengerRequest {
	private final MobsimPassengerAgent passenger;
	private final Link fromLink;
	private final Link toLink;
	
	private RequestImpl delegate ;

	public OneTaxiRequest(Id<Request> id, MobsimPassengerAgent passenger, Link fromLink, Link toLink,
			double submissionTime) {
		// I want a taxi now, i.e. earliestStartTime == latestStartTime == submissionTime
//		super(id, 1, submissionTime, submissionTime, submissionTime);
		delegate = new RequestImpl( id, 1, submissionTime, submissionTime, submissionTime ) ;
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
}
