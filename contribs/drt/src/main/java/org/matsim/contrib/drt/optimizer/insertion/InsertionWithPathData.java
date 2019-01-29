/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;

/**
 * @author michalm
 */
public class InsertionWithPathData implements InsertionWithDetourTimes {
	private final int pickupIdx;
	private final int dropoffIdx;
	private final PathData pathToPickup;
	private final PathData pathFromPickup;
	private final PathData pathToDropoff;// null if dropoff inserted directly after pickup
	private final PathData pathFromDropoff;// null if dropoff inserted at the end

	InsertionWithPathData(int pickupIdx, int dropoffIdx, PathData pathToPickup, PathData pathFromPickup,
			PathData pathToDropoff, PathData pathFromDropoff) {
		this.pickupIdx = pickupIdx;
		this.dropoffIdx = dropoffIdx;
		this.pathToPickup = pathToPickup;
		this.pathFromPickup = pathFromPickup;
		this.pathToDropoff = pathToDropoff;
		this.pathFromDropoff = pathFromDropoff;
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
		return pathToPickup.getTravelTime();
	}

	@Override
	public double getTimeFromPickup() {
		return pathFromPickup.getTravelTime();
	}

	@Override
	public double getTimeToDropoff() {
		return pathToDropoff.getTravelTime();// NullPointerException if dropoff inserted directly after pickup
	}

	@Override
	public double getTimeFromDropoff() {
		return pathFromDropoff.getTravelTime();// NullPointerException if dropoff inserted at the end
	}

	public PathData getPathToPickup() {
		return pathToPickup;
	}

	public PathData getPathFromPickup() {
		return pathFromPickup;
	}

	public PathData getPathToDropoff() {
		return pathToDropoff;
	}

	public PathData getPathFromDropoff() {
		return pathFromDropoff;
	}

	@Override
	public String toString() {
		return "Insertion: pickupIdx=" + pickupIdx + ", dropoffIdx=" + dropoffIdx;
	}
}
