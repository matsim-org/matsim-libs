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

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData.InsertionDetourData;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;

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
public class DetourPathData {
	private final Map<Link, PathData> detourToPickup;
	private final Map<Link, PathData> detourFromPickup;
	private final Map<Link, PathData> detourToDropoff;
	private final Map<Link, PathData> detourFromDropoff;
	private final PathData zeroDetour;

	DetourPathData(Map<Link, PathData> detourToPickup, Map<Link, PathData> detourFromPickup,
			Map<Link, PathData> detourToDropoff, Map<Link, PathData> detourFromDropoff, PathData zeroDetour) {
		this.detourToPickup = detourToPickup;
		this.detourFromPickup = detourFromPickup;
		this.detourToDropoff = detourToDropoff;
		this.detourFromDropoff = detourFromDropoff;
		this.zeroDetour = zeroDetour;
	}

	public InsertionDetourData<PathData> createInsertionDetourData(InsertionGenerator.Insertion insertion) {
		PathData toPickup = detourToPickup.get(insertion.pickup.previousWaypoint.getLink());
		PathData fromPickup = detourFromPickup.get(insertion.pickup.nextWaypoint.getLink());
		PathData toDropoff = insertion.dropoff.previousWaypoint instanceof Waypoint.Pickup ?
				null :
				detourToDropoff.get(insertion.dropoff.previousWaypoint.getLink());
		PathData fromDropoff = insertion.dropoff.nextWaypoint instanceof Waypoint.End ?
				zeroDetour :
				detourFromDropoff.get(insertion.dropoff.nextWaypoint.getLink());
		return new InsertionDetourData<>(toPickup, fromPickup, toDropoff, fromDropoff);
	}
}
