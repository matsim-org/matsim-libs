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

package org.matsim.contrib.dvrp.data;

import org.matsim.api.core.v01.Id;

/**
 * @author michalm
 */
public final class RequestImpl implements Request {
	private final Id<Request> id;
	private final double quantity;
	private final double earliestStartTime;
	private final double latestStartTime;
	private final double submissionTime;

	private boolean rejected = false;

	public RequestImpl(Id<Request> id, double quantity, double earliestStartTime, double latestStartTime,
			double submissionTime) {
		this.id = id;
		this.quantity = quantity;
		this.earliestStartTime = earliestStartTime;
		this.latestStartTime = latestStartTime;
		this.submissionTime = submissionTime;
	}

	@Override
	public Id<Request> getId() {
		return id;
	}

	@Override
	public double getQuantity() {
		return quantity;
	}

	@Override
	public double getEarliestStartTime() {
		return earliestStartTime;
	}

	@Override
	public double getLatestStartTime() {
		return latestStartTime;
	}

	@Override
	public double getSubmissionTime() {
		return submissionTime;
	}

	@Override
	public boolean isRejected() {
		return rejected;
	}

	public void setRejected(boolean rejected) {
		this.rejected = rejected;
	}

	@Override
	public String toString() {
		return "Request_" + id + " [TW=(" + earliestStartTime + ", " + latestStartTime + ")]";
	}
}