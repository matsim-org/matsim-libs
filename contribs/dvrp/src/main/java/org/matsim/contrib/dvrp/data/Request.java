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
 * @author michalm
 */
public interface Request extends Identifiable<Request> {

	/**
	 * @return the amount of people/goods to serve/transport (see: {@link Vehicle#getCapacity()})
	 */
	double getQuantity();

	/**
	 * @return begining of the time window (inclusive) - earliest time when the request can be served
	 */
	double getEarliestStartTime();

	/**
	 * @return end of the time window (exclusive) - time by which the request should be served
	 */
	double getLatestStartTime();

	/**
	 * @return time at which the request was submitted
	 */
	double getSubmissionTime();

	/**
	 * @return indicates whether the request has been rejected by the service provider (optimizer/dispatcher)
	 */
	boolean isRejected();
}
