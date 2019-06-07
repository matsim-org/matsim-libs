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

package org.matsim.contrib.dvrp.optimizer;

import org.matsim.api.core.v01.Identifiable;

/**
 * Represents a general request in DVRP.
 *
 * @author michalm
 */
public interface Request extends Identifiable<Request> {

	/**
	 * @return time at which the request was submitted
	 */
	double getSubmissionTime();

	static String toString(Request request) {
		return "[id=" + request.getId() + "][submissionTime=" + request.getSubmissionTime() + "]";
	}
}
