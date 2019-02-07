/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer.fifo;

import java.util.Collection;
import java.util.Iterator;

import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.taxi.passenger.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;

public class FifoSchedulingProblem {
	private final Fleet fleet;
	private final TaxiScheduler scheduler;
	private final BestDispatchFinder dispatchFinder;

	public FifoSchedulingProblem(Fleet fleet, TaxiScheduler scheduler, BestDispatchFinder vrpFinder) {
		this.fleet = fleet;
		this.scheduler = scheduler;
		this.dispatchFinder = vrpFinder;
	}

	public void scheduleUnplannedRequests(Collection<TaxiRequest> unplannedRequests) {
		Iterator<TaxiRequest> reqIter = unplannedRequests.iterator();
		while (reqIter.hasNext()) {
			TaxiRequest req = reqIter.next();

			BestDispatchFinder.Dispatch<TaxiRequest> best = dispatchFinder.findBestVehicleForRequest(req,
					fleet.getVehicles().values().stream());

			// TODO search only through available vehicles
			// TODO what about k-nearstvehicle filtering?

			if (best == null) {// TODO won't work with req filtering; use VehicleData to find out when to exit???
				return;
			}

			scheduler.scheduleRequest(best.vehicle, best.destination, best.path);
			reqIter.remove();
		}
	}
}
