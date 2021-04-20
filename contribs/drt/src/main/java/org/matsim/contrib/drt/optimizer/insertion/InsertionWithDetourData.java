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

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.InsertionPoint;

import com.google.common.base.MoreObjects;

/**
 * @author michalm
 */
public class InsertionWithDetourData<D> {
	private final Insertion insertion;

	private final D detourToPickup;
	private final D detourFromPickup; // "zero" detour if pickup inserted at the end of schedule !!!
	private final D detourToDropoff; // detour from pickup if dropoff inserted directly after pickup
	private final D detourFromDropoff; // "zero" detour if dropoff inserted at the end of schedule

	InsertionWithDetourData(Insertion insertion, D detourToPickup, D detourFromPickup, D detourToDropoff,
			D detourFromDropoff) {
		this.insertion = insertion;
		this.detourToPickup = detourToPickup;
		this.detourFromPickup = detourFromPickup;
		this.detourToDropoff = detourToDropoff;
		this.detourFromDropoff = detourFromDropoff;
	}

	public Insertion getInsertion() {
		return insertion;
	}

	public VehicleEntry getVehicleEntry() {
		return insertion.vehicleEntry;
	}

	public InsertionPoint getPickup() {
		return insertion.pickup;
	}

	public InsertionPoint getDropoff() {
		return insertion.dropoff;
	}

	/**
	 * Detour necessary to get from start or the preceding stop to pickup.
	 * <p>
	 * If pickup is inserted at the (existing) previous stop -> no detour.
	 *
	 * @return
	 */
	public D getDetourToPickup() {
		return detourToPickup;
	}

	/**
	 * Detour necessary to get from pickup to the next stop or 0 if appended at the end.
	 * <p>
	 * IMPORTANT: At this point the dropoff location is not taken into account !!!
	 *
	 * @return
	 */
	public D getDetourFromPickup() {
		return detourFromPickup;
	}

	/**
	 * Detour necessary to get from the preceding stop (could be a stop of the corresponding pickup) to dropoff.
	 * <p>
	 * If dropoff is inserted at the (existing) previous stop -> no detour.
	 *
	 * @return
	 */
	public D getDetourToDropoff() {
		return detourToDropoff;
	}

	/**
	 * Detour necessary to get from dropoff to the next stop or no detour if appended at the end.
	 *
	 * @return
	 */
	public D getDetourFromDropoff() {
		return detourFromDropoff;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("insertion", insertion)
				.add("detourToPickup", detourToPickup)
				.add("detourFromPickup", detourFromPickup)
				.add("detourToDropoff", detourToDropoff)
				.add("detourFromDropoff", detourFromDropoff)
				.toString();
	}
}
