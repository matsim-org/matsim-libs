/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
package org.matsim.contrib.drt.passenger;

import java.util.Optional;

public class DefaultOfferAcceptor implements DrtOfferAcceptor{

	@Override
	public Optional<AcceptedDrtRequest> acceptDrtOffer(DrtRequest request, double departureTime, double arrivalTime, double dropoffDuration) {
		double updatedLatestStartTime = Math.min(departureTime
			+ request.getConstraints().maxPickupDelay(), request.getLatestStartTime());
		return Optional.of(AcceptedDrtRequest
			.newBuilder()
			.request(request)
			.earliestStartTime(request.getEarliestStartTime())
			.maxRideDuration(request.getConstraints().maxRideDuration())
			.latestArrivalTime(request.getConstraints().latestArrivalTime())
			.latestStartTime(updatedLatestStartTime)
			.dropoffDuration(dropoffDuration)
			.plannedPickupTime(departureTime)
			.plannedDropoffTime(arrivalTime)
			.build());
	}
}
