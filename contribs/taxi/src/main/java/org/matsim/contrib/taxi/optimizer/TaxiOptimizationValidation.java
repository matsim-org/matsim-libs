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

package org.matsim.contrib.taxi.optimizer;

import java.util.stream.Stream;

import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.taxi.data.TaxiRequest.TaxiRequestStatus;
import org.matsim.contrib.taxi.data.TaxiRequests;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;

public class TaxiOptimizationValidation {
	public static void assertNoUnplannedRequestsWhenIdleVehicles(TaxiScheduler taxiScheduler, Fleet fleet,
			Stream<? extends Request> requests) {
		if (fleet.getVehicles().values().stream().filter(taxiScheduler::isIdle).count() > 0
				&& TaxiRequests.countRequestsWithStatus(requests, TaxiRequestStatus.UNPLANNED) > 0) {
			throw new IllegalStateException("idle vehicles and unplanned requests");
		}
	}
}
