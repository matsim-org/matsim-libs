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
 * Constraints of a single DRT request, all durations in seconds.
 *
 * @param maxTravelDuration      maximum time from the earliest departure to the dropoff, i.e. wait time plus ride time;
 *                               defines the request's latest arrival time
 * @param maxRideDuration        maximum time from the end of the pickup to the dropoff (in-vehicle time), independent
 *                               of the wait time
 * @param maxWaitDuration        maximum time from the earliest departure to the pickup; defines the request's latest
 *                               start time at submission
 * @param maxPickupDelay         maximum delay of the pickup relative to the pickup time planned when the offer is
 *                               accepted; the default offer acceptor tightens the latest start time to
 *                               min(planned pickup + maxPickupDelay, earliest departure + maxWaitDuration)
 * @param lateDiversionThreshold vehicles are not diverted for new requests once an on-board passenger is less than this
 *                               time away from their dropoff (0 disables the check)
 * @param allowRejection         if true, violations of the above constraints make an insertion infeasible; if false,
 *                               they are penalised in the insertion cost
 *
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
