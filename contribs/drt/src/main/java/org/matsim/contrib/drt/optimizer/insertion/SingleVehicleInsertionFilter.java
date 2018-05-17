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

package org.matsim.contrib.drt.optimizer.insertion;

import java.util.List;
import java.util.stream.Collectors;

import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.insertion.DetourTimesProvider.DetourTimesSet;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;

/**
 * @author michalm
 */
public class SingleVehicleInsertionFilter {
	private final DetourTimesProvider detourTimesProvider;
	private final InsertionCostCalculator costCalculator;

	public SingleVehicleInsertionFilter(DetourTimesProvider detourTimesProvider,
			InsertionCostCalculator costCalculator) {
		this.detourTimesProvider = detourTimesProvider;
		this.costCalculator = costCalculator;
	}

	public List<InsertionWithDetourTimes> findFeasibleInsertions(DrtRequest drtRequest, VehicleData.Entry vEntry,
			List<Insertion> insertions) {
		DetourTimesSet set = detourTimesProvider.getDetourTimesSet(drtRequest, vEntry);
		int stopCount = vEntry.stops.size();

		return insertions.stream()//
				.map(i -> createInsertionWithDetourTimes(i, set, stopCount))//
				.filter(iWithDetourTimes -> costCalculator.calculate(drtRequest, vEntry,
						iWithDetourTimes) < InsertionCostCalculator.INFEASIBLE_SOLUTION_COST)//
				.collect(Collectors.toList());
	}

	private InsertionWithDetourTimes createInsertionWithDetourTimes(Insertion insertion, DetourTimesSet set,
			int stopCount) {
		int i = insertion.pickupIdx;
		int j = insertion.dropoffIdx;

		// i -> pickup
		Double toPickup = set.timesToPickup[i]; // i -> pickup
		Double fromPickup = set.timesFromPickup[i == j ? 0 : i + 1]; // pickup -> (dropoff | i+1)
		Double toDropoff = i == j ? null // pickup followed by dropoff
				: set.timesToDropoff[j]; // j -> dropoff
		Double fromDropoff = j == stopCount ? null // dropoff inserted at the end
				: set.timesFromDropoff[j + 1];
		return new InsertionWithDetourTimesImpl(i, j, toPickup, fromPickup, toDropoff, fromDropoff);
	}

	private static class InsertionWithDetourTimesImpl implements InsertionWithDetourTimes {
		private final int pickupIdx;
		private final int dropoffIdx;
		private final Double timeToPickup;
		private final Double timeFromPickup;
		private final Double timeToDropoff;// null if dropoff inserted directly after pickup
		private final Double timeFromDropoff;// null if dropoff inserted at the end

		public InsertionWithDetourTimesImpl(int pickupIdx, int dropoffIdx, Double timeToPickup, Double timeFromPickup,
				Double timeToDropoff, Double timeFromDropoff) {
			this.pickupIdx = pickupIdx;
			this.dropoffIdx = dropoffIdx;
			this.timeToPickup = timeToPickup;
			this.timeFromPickup = timeFromPickup;
			this.timeToDropoff = timeToDropoff;
			this.timeFromDropoff = timeFromDropoff;
		}

		@Override
		public int getPickupIdx() {
			return pickupIdx;
		}

		@Override
		public int getDropoffIdx() {
			return dropoffIdx;
		}

		@Override
		public double getTimeToPickup() {
			return timeToPickup;
		}

		@Override
		public double getTimeFromPickup() {
			return timeFromPickup;
		}

		@Override
		public double getTimeToDropoff() {
			return timeToDropoff;
		}

		@Override
		public double getTimeFromDropoff() {
			return timeFromDropoff;
		}

		@Override
		public String toString() {
			return "[pickupIdx=" + pickupIdx + "][dropoffIdx=" + dropoffIdx + "]";
		}
	}
}
