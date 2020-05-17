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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;

/**
 * @author Michal Maciejewski (michalm)
 */
public interface DetourDataProvider<D> {

	static <D> DetourDataSet<D> getDetourDataSet(DrtRequest drtRequest, VehicleData.Entry vEntry,
			Function<Link, D> pathsToPickupMap, Function<Link, D> pathsFromPickupMap,
			Function<Link, D> pathsToDropoffMap, Function<Link, D> pathsFromDropoffMap) {

		int length = vEntry.stops.size() + 1;
		List<D> pathsToPickup = new ArrayList<>(length);
		List<D> pathsFromPickup = new ArrayList<>(length);
		List<D> pathsToDropoff = new ArrayList<>(length);
		List<D> pathsFromDropoff = new ArrayList<>(length);

		//FIXME update to follow changes in DetourDataSet
		pathsToPickup.add(pathsToPickupMap.apply(vEntry.start.link));// start->pickup
		pathsFromPickup.add(pathsFromPickupMap.apply(drtRequest.getToLink()));// pickup->dropoff
		pathsToDropoff.add(null);
		pathsFromDropoff.add(null);

		for (VehicleData.Stop s : vEntry.stops) {
			Link link = s.task.getLink();
			pathsToPickup.add(pathsToPickupMap.apply(link));
			pathsFromPickup.add(pathsFromPickupMap.apply(link));
			pathsToDropoff.add(pathsToDropoffMap.apply(link));
			pathsFromDropoff.add(pathsFromDropoffMap.apply(link));
		}

		return new DetourDataSet<>(pathsToPickup, pathsFromPickup, pathsToDropoff, pathsFromDropoff);
	}

	/**
	 * Contains detour data for all potential insertions (i.e. pickup and dropoff indices)
	 * <p>
	 * Having them collected in one set allows the typical use case where all paths are precomputed in one go
	 * and then provided via InsertionWithPathData for a specific Insertion
	 */
	class DetourDataSet<D> {
		// detour[0] is a special entry; detour[i > 0] corresponds to stop i-1, for 1 <= i <= stopCount
		private final List<D> detourToPickup; //detour[0] start->pickup
		private final List<D> detourFromPickup; //detour[0] pickup at the end
		private final List<D> detourToDropoff; //detour[0] pickup->dropoff
		private final List<D> detourFromDropoff; //detour[0] dropoff at the end

		private DetourDataSet(List<D> detourToPickup, List<D> detourFromPickup, List<D> detourToDropoff,
				List<D> detourFromDropoff) {
			this.detourToPickup = detourToPickup;
			this.detourFromPickup = detourFromPickup;
			this.detourToDropoff = detourToDropoff;
			this.detourFromDropoff = detourFromDropoff;
		}

		public InsertionWithDetourData<D> createInsertionDetourData(Insertion insertion) {
			int i = insertion.pickupIdx;
			int j = insertion.dropoffIdx;

			// i -> pickup
			D toPickup = detourToPickup.get(i); // i -> pickup
			D fromPickup = detourFromPickup.get(i == j ? 0 : i + 1); // pickup -> (dropoff | i+1)
			D toDropoff = i == j ? null // pickup followed by dropoff
					: detourToDropoff.get(j); // j -> dropoff
			D fromDropoff = j == detourFromDropoff.size() - 1 ? null // dropoff inserted at the end
					: detourFromDropoff.get(j + 1);

			// TODO switch to the new approach
			//			D fromPickup = i == detourFromPickup.length //
			//					? detourFromPickup[0] // pickup inserted at the end
			//					: detourFromPickup[i + 1]; // pickup -> i+1
			//			D toDropoff = i == j ? detourFromPickup[0] // pickup followed by dropoff
			//					: detourToDropoff[j]; // j -> dropoff
			//			D fromDropoff = j == detourFromDropoff.length //
			//					? detourFromDropoff[0] // dropoff inserted at the end
			//					: detourFromDropoff[j + 1];

			return new InsertionWithDetourData<>(i, j, toPickup, fromPickup, toDropoff, fromDropoff);
		}
	}

	DetourDataSet<D> getDetourDataSet(DrtRequest drtRequest, VehicleData.Entry vEntry);
}
