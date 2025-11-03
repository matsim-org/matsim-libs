/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2024 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.constraints;

/**
 * @author Sebastian Hörl, IRT SystemX
 * @author nkuehnel / MOIA
 */
public record DrtRouteConstraints(
		double maxTravelDuration,
		double maxRideDuration,
		double maxWaitDuration,
		double maxPickupDelay,
		double lateDiversionThreshold,
		boolean allowRejection
) {

	public final static DrtRouteConstraints UNDEFINED =
			new DrtRouteConstraints(
					Double.POSITIVE_INFINITY,
					Double.POSITIVE_INFINITY,
					Double.POSITIVE_INFINITY,
					Double.POSITIVE_INFINITY,
					0,
					false
			);
}
