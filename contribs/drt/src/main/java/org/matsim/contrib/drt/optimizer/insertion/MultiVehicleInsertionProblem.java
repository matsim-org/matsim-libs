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

import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.insertion.SingleVehicleInsertionProblem.BestInsertion;

/**
 * @author michalm
 */
public class MultiVehicleInsertionProblem {
	private final SingleVehicleInsertionProblem insertionProblem;

	public MultiVehicleInsertionProblem(SingleVehicleInsertionProblem insertionProblem) {
		this.insertionProblem = insertionProblem;
	}

	public BestInsertion findBestInsertion(DrtRequest drtRequest, VehicleData vData) {
		return findBestInsertion(drtRequest, vData.getEntries());
	}

	// TODO run Dijkstra once for all vehicles instead of running it separately for each one
	public BestInsertion findBestInsertion(DrtRequest drtRequest, Iterable<Entry> vEntries) {
		double minCost = Double.MAX_VALUE;
		BestInsertion fleetBestInsertion = null;
		for (Entry vEntry : vEntries) {
			BestInsertion bestInsertion = insertionProblem.findBestInsertion(drtRequest, vEntry);
			if (bestInsertion.cost < minCost) {
				fleetBestInsertion = bestInsertion;
				minCost = bestInsertion.cost;
			}
		}
		return fleetBestInsertion;
	}
}
