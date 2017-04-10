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

package playground.michalm.drt.optimizer.insertion;

import playground.michalm.drt.data.NDrtRequest;
import playground.michalm.drt.optimizer.VehicleData;
import playground.michalm.drt.optimizer.VehicleData.Entry;
import playground.michalm.drt.optimizer.insertion.SingleVehicleInsertionProblem.BestInsertion;

/**
 * @author michalm
 */
public class MultiVehicleInsertionProblem {
	private final SingleVehicleInsertionProblem insertionProblem;

	public MultiVehicleInsertionProblem(SingleVehicleInsertionProblem insertionProblem) {
		this.insertionProblem = insertionProblem;
	}

	public BestInsertion findBestInsertion(NDrtRequest drtRequest, VehicleData vData) {
		return findBestInsertion(drtRequest, vData.getEntries());
	}

	//TODO run Dijkstra once for all vehicles instead of running it separately for each one
	public BestInsertion findBestInsertion(NDrtRequest drtRequest, Iterable<Entry> vEntries) {
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
