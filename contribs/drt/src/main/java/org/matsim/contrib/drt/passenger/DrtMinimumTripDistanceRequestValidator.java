/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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

import java.util.Set;

import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.routing.MinimumTripDistanceAccessEgressFacilityFinder;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;

/**
 * Rejects requests whose access and egress links are closer to each other than a minimum distance.
 * <p>
 * The minimum trip distance is primarily enforced during routing (see
 * {@link MinimumTripDistanceAccessEgressFacilityFinder}), where too short trips are not routed at all. This validator
 * is the safety net for requests that reach the optimizer without having been routed in this run (prebooking,
 * within-day replanning, input plans carrying finished DRT routes). A rejection aborts the agent, so it should remain
 * the exception.
 * <p>
 * The validator does not know the person or the trip attributes, so it cannot ask the
 * {@link org.matsim.contrib.drt.optimizer.constraints.ConstraintSetChooser} which constraints set applies. It therefore
 * applies one distance for all requests, and {@link #forConstraintsSets} uses the smallest minimum over all sets. This
 * way, the validator never rejects a trip that the routing accepted.
 *
 * @author nkuehnel / MOIA
 */
public class DrtMinimumTripDistanceRequestValidator implements PassengerRequestValidator {

	public static final String BELOW_MINIMUM_TRIP_DISTANCE_CAUSE = "below_minimum_trip_distance";

	private final double minimumTripDistance;

	public DrtMinimumTripDistanceRequestValidator(double minimumTripDistance) {
		this.minimumTripDistance = minimumTripDistance;
	}

	/**
	 * Creates a validator with the smallest minimum trip distance of the given constraints sets.
	 */
	public static DrtMinimumTripDistanceRequestValidator forConstraintsSets(
			Iterable<? extends DrtOptimizationConstraintsSet> constraintsSets) {
		double smallestMinimum = Double.POSITIVE_INFINITY;
		for (DrtOptimizationConstraintsSet constraintsSet : constraintsSets) {
			smallestMinimum = Math.min(smallestMinimum, constraintsSet.getMinimumTripDistance());
		}
		return new DrtMinimumTripDistanceRequestValidator(Double.isInfinite(smallestMinimum) ? 0 : smallestMinimum);
	}

	public double getMinimumTripDistance() {
		return minimumTripDistance;
	}

	@Override
	public Set<String> validateRequest(PassengerRequest request) {
		double tripDistance = MinimumTripDistanceAccessEgressFacilityFinder.calcTripDistance(request.getFromLink(),
				request.getToLink());
		return tripDistance < minimumTripDistance ? Set.of(BELOW_MINIMUM_TRIP_DISTANCE_CAUSE) : Set.of();
	}
}
