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
public class InsertionWithPathData {
	public final int pickupIdx;
	public final int dropoffIdx;
	public final PathData pathToPickup;
	public final PathData pathFromPickup;
	public final PathData pathToDropoff;// null if dropoff inserted directly after pickup
	public final PathData pathFromDropoff;// null if dropoff inserted at the end

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
	public String toString() {
		return "Insertion: pickupIdx=" + pickupIdx + ", dropoffIdx=" + dropoffIdx;
	}
}
