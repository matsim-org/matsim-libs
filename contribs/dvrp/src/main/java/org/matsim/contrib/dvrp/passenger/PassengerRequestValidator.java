/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.passenger;

import java.util.Set;

/**
 * Validates (for the optimizer), whether a PassengerRequest should be served or not (e.g. for limitations in business
 * area or distance or time)
 *
 * @author jbischoff
 * @author michalm (Michal Maciejewski)
 */
public interface PassengerRequestValidator {
	/**
	 * Checks if the request can be served given some spatiotemporal (limited time and area of operations) or other
	 * constraints.
	 * <p>
	 * Preferred format for causes: underscores instead of spaces.
	 *
	 * @param request to be validated
	 * @return set containing causes of constraint violations. An empty set means the request fulfills all
	 * constraints and may be considered by the optimizer (although this does not guarantee it will get scheduled)
	 */
	Set<String> validateRequest(PassengerRequest request);
}
