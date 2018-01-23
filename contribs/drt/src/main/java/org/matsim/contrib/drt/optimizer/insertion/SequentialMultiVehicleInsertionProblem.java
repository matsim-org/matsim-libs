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

import java.util.Collection;

import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.insertion.SingleVehicleInsertionProblem.BestInsertion;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
public class SequentialMultiVehicleInsertionProblem implements MultiVehicleInsertionProblem {
	private final SingleVehicleInsertionProblem insertionProblem;

	public SequentialMultiVehicleInsertionProblem(PathDataProvider pathDataProvider, DrtConfigGroup drtCfg,
			MobsimTimer timer) {
		this.insertionProblem = new SingleVehicleInsertionProblem(pathDataProvider, drtCfg, timer);
	}

	// TODO run Dijkstra once for all vehicles instead of running it separately for each one
	@Override
	public BestInsertion findBestInsertion(DrtRequest drtRequest, Collection<Entry> vEntries) {
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
