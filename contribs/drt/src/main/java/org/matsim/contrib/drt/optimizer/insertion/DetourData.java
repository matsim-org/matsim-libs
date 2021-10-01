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

import java.util.Map;
import java.util.function.Function;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.passenger.DrtRequest;

/**
 * Contains detour data for all potential insertions (i.e. pickup and dropoff indices)
 * <p>
 * Having them collected in one set allows the typical use case where all paths are precomputed in one go
 * and then provided via InsertionWithPathData for a specific Insertion.
 * <p>
 * The current implementation assumes the DetourData functions are time independent. This may be changed in the future (esp.
 * for pre-booking or to enhance simple beeline TT estimation) to BiFunctions: (Link, time) -> data.
 * <p>
 * On the other hand, detour data (D) could itself provide time-dependent information.
 */
public class DetourData<D> {
	static DetourData<Double> create(DetourTimeEstimator detourTimeEstimator, DrtRequest drtRequest) {
		//TODO add departure/arrival times to improve estimation
		Function<Link, Double> timesToPickup = link -> detourTimeEstimator.estimateTime(link, drtRequest.getFromLink());
		Function<Link, Double> timesFromPickup = link -> detourTimeEstimator.estimateTime(drtRequest.getFromLink(),
				link);
		Function<Link, Double> timesToDropoff = link -> detourTimeEstimator.estimateTime(link, drtRequest.getToLink());
		Function<Link, Double> timesFromDropoff = link -> detourTimeEstimator.estimateTime(drtRequest.getToLink(),
				link);
		return new DetourData<>(timesToPickup, timesFromPickup, timesToDropoff, timesFromDropoff, 0.);
	}

	private final Function<Link, D> detourToPickup;
	private final Function<Link, D> detourFromPickup;
	private final Function<Link, D> detourToDropoff;
	private final Function<Link, D> detourFromDropoff;
	private final D zeroDetour;

	DetourData(Map<Link, D> detourToPickup, Map<Link, D> detourFromPickup, Map<Link, D> detourToDropoff,
			Map<Link, D> detourFromDropoff, D zeroDetour) {
		this(detourToPickup::get, detourFromPickup::get, detourToDropoff::get, detourFromDropoff::get, zeroDetour);
	}

	DetourData(Function<Link, D> detourToPickup, Function<Link, D> detourFromPickup, Function<Link, D> detourToDropoff,
			Function<Link, D> detourFromDropoff, D zeroDetour) {
		this.detourToPickup = detourToPickup;
		this.detourFromPickup = detourFromPickup;
		this.detourToDropoff = detourToDropoff;
		this.detourFromDropoff = detourFromDropoff;
		this.zeroDetour = zeroDetour;
	}

	public InsertionWithDetourData<D> createInsertionWithDetourData(InsertionGenerator.Insertion insertion) {
		D toPickup = detourToPickup.apply(insertion.pickup.previousWaypoint.getLink());
		D fromPickup = detourFromPickup.apply(insertion.pickup.nextWaypoint.getLink());
		D toDropoff = insertion.dropoff.previousWaypoint instanceof Waypoint.Pickup ?
				null :
				detourToDropoff.apply(insertion.dropoff.previousWaypoint.getLink());
		D fromDropoff = insertion.dropoff.nextWaypoint instanceof Waypoint.End ?
				zeroDetour :
				detourFromDropoff.apply(insertion.dropoff.nextWaypoint.getLink());
		return new InsertionWithDetourData<>(insertion, toPickup, fromPickup, toDropoff, fromDropoff);
	}
}
