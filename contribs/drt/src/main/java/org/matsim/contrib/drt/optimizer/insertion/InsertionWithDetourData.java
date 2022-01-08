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

import static org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;

import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;

import com.google.common.base.MoreObjects;

/**
 * @author michalm
 */
public class InsertionWithDetourData<D> {
	public static class InsertionDetourData<D> {
		/**
		 * Detour necessary to get from start or the preceding stop to pickup.
		 * <p>
		 * If pickup is inserted at the (existing) previous stop -> no detour.
		 */
		public final D detourToPickup;
		/**
		 * Detour necessary to get from pickup to the next stop or 0 if appended at the end.
		 * <p>
		 * IMPORTANT: At this point the dropoff location is not taken into account !!!
		 * "zero" detour if pickup inserted at the end of schedule !!!
		 */
		public final D detourFromPickup;
		/**
		 * Detour necessary to get from the preceding stop (could be a stop of the corresponding pickup) to dropoff.
		 * <p>
		 * If dropoff is inserted at the (existing) previous stop -> no detour.
		 * If dropoff inserted directly after pickup -> detour from pickup
		 */
		public final D detourToDropoff;
		/**
		 * Detour necessary to get from dropoff to the next stop or no detour if appended at the end.
		 * <p>
		 * "zero" detour if dropoff inserted at the end of schedule
		 */
		public final D detourFromDropoff;

		public InsertionDetourData(D detourToPickup, D detourFromPickup, D detourToDropoff, D detourFromDropoff) {
			this.detourToPickup = detourToPickup;
			this.detourFromPickup = detourFromPickup;
			this.detourToDropoff = detourToDropoff;
			this.detourFromDropoff = detourFromDropoff;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("detourToPickup", detourToPickup)
					.add("detourFromPickup", detourFromPickup)
					.add("detourToDropoff", detourToDropoff)
					.add("detourFromDropoff", detourFromDropoff)
					.toString();
		}
	}

	private final Insertion insertion;
	private final InsertionDetourData<D> insertionDetourData;
	private final DetourTimeInfo detourTimeInfo;

	InsertionWithDetourData(Insertion insertion, InsertionDetourData<D> insertionDetourData,
			DetourTimeInfo detourTimeInfo) {
		this.insertion = insertion;
		this.insertionDetourData = insertionDetourData;
		this.detourTimeInfo = detourTimeInfo;
	}

	public Insertion getInsertion() {
		return insertion;
	}

	public InsertionDetourData<D> getDetourData() {
		return insertionDetourData;
	}

	public DetourTimeInfo getDetourTimeInfo() {
		return detourTimeInfo;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("insertion", insertion)
				.add("insertionDetourData", insertionDetourData)
				.add("detourTimeInfo", detourTimeInfo)
				.toString();
	}
}
