/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.data.validator;

import java.util.Set;

import org.matsim.contrib.drt.data.DrtRequest;

/**
 * Validates (for the optimizer), whether a DRTRequest should be served or not (e.g. for limitations in business area or
 * distance or time)
 *
 * @author jbischoff
 */
public interface DrtRequestValidator {
	/**
	 * Checks if the request can be served given some spatiotemporal (limited time and area of operations) or other constraints.
	 * <p>
	 * Preferred format for causes: underscores instead of spaces.
	 *
	 * @param request to be validated
	 * @return set containing causes of constraint violations. An empty set means the request fulfills all
	 * constraints and may be considered by the optimizer (although this does not guarantee it will get scheduled)
	 */
	Set<String> validateDrtRequest(DrtRequest request);
}
