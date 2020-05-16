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

import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;

/**
 * @author Michal Maciejewski (michalm)
 */
public interface DetourDataProvider<D> {

	/**
	 * Contains detour data for all potential insertions (i.e. pickup and dropoff indices)
	 * <p>
	 * Having them collected in one set allows the typical use case where all paths are precomputed in one go
	 * and then provided via InsertionWithPathData for a specific Insertion
	 */
	class DetourDataSet<D> {
		// detour[0] is a special entry; detour[i > 0] corresponds to stop i-1, for 1 <= i <= stopCount
		private final D[] detourToPickup; //detour[0] start->pickup
		private final D[] detourFromPickup; //detour[0] pickup at the end
		private final D[] detourToDropoff; //detour[0] pickup->dropoff
		private final D[] detourFromDropoff; //detour[0] dropoff at the end

		DetourDataSet(D[] detourToPickup, D[] detourFromPickup, D[] detourToDropoff, D[] detourFromDropoff) {
			this.detourToPickup = detourToPickup;
			this.detourFromPickup = detourFromPickup;
			this.detourToDropoff = detourToDropoff;
			this.detourFromDropoff = detourFromDropoff;
		}

		public InsertionWithDetourData<D> createInsertionDetourData(Insertion insertion) {
			int i = insertion.pickupIdx;
			int j = insertion.dropoffIdx;

			// i -> pickup
			D toPickup = detourToPickup[i]; // i -> pickup
			D fromPickup = detourFromPickup[i == j ? 0 : i + 1]; // pickup -> (dropoff | i+1)
			D toDropoff = i == j ? null // pickup followed by dropoff
					: detourToDropoff[j]; // j -> dropoff
			D fromDropoff = j == detourFromDropoff.length - 1 ? null // dropoff inserted at the end
					: detourFromDropoff[j + 1];

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
