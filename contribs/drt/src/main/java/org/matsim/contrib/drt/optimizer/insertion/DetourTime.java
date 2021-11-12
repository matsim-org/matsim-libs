/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.insertion;

import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.passenger.DrtRequest;

/**
 * The current implementation assumes the DetourData functions are time independent. This may be changed in the future (esp.
 * for pre-booking or to enhance TT estimation) to BiFunctions: (Link, time) -> data.
 */
public class DetourTime {
	//TODO add departure/arrival times to improve estimation
	private final DetourTimeEstimator detourTimeEstimator;

	DetourTime(DetourTimeEstimator detourTimeEstimator) {
		this.detourTimeEstimator = detourTimeEstimator;
	}

	public double calcToPickupTime(InsertionGenerator.Insertion insertion, DrtRequest drtRequest) {
		return detourTimeEstimator.estimateTime(insertion.pickup.previousWaypoint.getLink(), drtRequest.getFromLink());
	}

	public double calcFromPickupTime(InsertionGenerator.Insertion insertion, DrtRequest drtRequest) {
		return detourTimeEstimator.estimateTime(drtRequest.getFromLink(), insertion.pickup.nextWaypoint.getLink());
	}

	public double calcToDropoffTime(InsertionGenerator.Insertion insertion, DrtRequest drtRequest) {
		return insertion.dropoff.previousWaypoint instanceof Waypoint.Pickup ?
				Double.POSITIVE_INFINITY :
				detourTimeEstimator.estimateTime(insertion.dropoff.previousWaypoint.getLink(), drtRequest.getToLink());
	}

	public double calcFromDropoffTime(InsertionGenerator.Insertion insertion, DrtRequest drtRequest) {
		return insertion.dropoff.nextWaypoint instanceof Waypoint.End ?
				0 :
				detourTimeEstimator.estimateTime(drtRequest.getToLink(), insertion.dropoff.nextWaypoint.getLink());
	}
}
