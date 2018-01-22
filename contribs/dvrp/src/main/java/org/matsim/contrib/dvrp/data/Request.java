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

import org.matsim.api.core.v01.Identifiable;

/**
 * Represents a request in DVRP.
 * 
 * Default methods are typical to MATSim scenarios: a single agent (quantity = 1) wants to depart immediately
 * (earliestStartTime = submissionTime), and will wait until the vehicle arrives (latestStartTime =
 * {@link Double#MAX_VALUE})
 * 
 * For a capacitated VRP - adapt getQuantity()
 * 
 * For a VRP with time windows - adapt both getEarliestStartTime() and getLatestStartTime()
 * 
 * For a VRP with rejections - implement isRejected()
 * 
 * @author michalm
 */
public interface Request extends Identifiable<Request> {

	/**
	 * @return the amount of people/goods to serve/transport (see: {@link Vehicle#getCapacity()})
	 */
	default double getQuantity() {
		return 1;
	}

	/**
	 * @return beginning of the time window (inclusive) - earliest time when the request can be served
	 */
	double getEarliestStartTime();

	/**
	 * @return end of the time window (exclusive) - time by which the request should be served
	 */
	default double getLatestStartTime() {
		return Double.MAX_VALUE;
	}

	/**
	 * @return time at which the request was submitted
	 */
	double getSubmissionTime();

	/**
	 * @return indicates whether the request has been rejected by the service provider (optimizer/dispatcher)
	 */
	default boolean isRejected() {
		return false;
	}

	public static String toString(Request request) {
		return "[id=" + request.getId() + "][submissionTime=" + request.getSubmissionTime() + "][earliestStartTime="
				+ request.getEarliestStartTime() + "]";
	}
}
