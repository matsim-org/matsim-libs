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

package org.matsim.contrib.taxi.optimizer.assignment;

import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.taxi.optimizer.VehicleData;
import org.matsim.contrib.taxi.optimizer.VehicleData.Entry;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentDestinationData.DestEntry;
import org.matsim.contrib.taxi.optimizer.assignment.VehicleAssignmentProblem.AssignmentCost;
import org.matsim.contrib.taxi.passenger.TaxiRequest;

public class TaxiToRequestAssignmentCostProvider {
	// This paper used ARRIVAL_TIME: M. Maciejewski, J. Bischoff, K. Nagel: An assignment-based approach to efficient
	// real-time city-scale taxi dispatching. IEEE Intelligent Systems, 2016.

	public enum Mode {
		PICKUP_TIME, //
		// TTki

		ARRIVAL_TIME, //
		// DEPk + TTki
		// equivalent to REMAINING_WAIT_TIME, i.e. DEPk + TTki - Tcurr // TODO check this out for dummy cases

		TOTAL_WAIT_TIME, //
		// DEPk + TTki - T0i

		DSE;//
		// balance between demand (ARRIVAL_TIME) and supply (PICKUP_TIME)
	}

	;

	private final AssignmentTaxiOptimizerParams params;

	public TaxiToRequestAssignmentCostProvider(AssignmentTaxiOptimizerParams params) {
		this.params = params;
	}

	public AssignmentCost<TaxiRequest> getCost(AssignmentRequestData rData, VehicleData vData) {
		final Mode currentMode = getCurrentMode(rData, vData);
		return new AssignmentCost<TaxiRequest>() {
			public double calc(Entry departure, DestEntry<TaxiRequest> reqEntry, PathData pathData) {
				double pickupBeginTime = calcPickupBeginTime(departure, reqEntry, pathData);
				switch (currentMode) {
					case PICKUP_TIME:
						// this will work different than ARRIVAL_TIME at oversupply -> will reduce T_P and fairness
						return pickupBeginTime - departure.time;

					case ARRIVAL_TIME:
						// less fairness, higher throughput
						return pickupBeginTime;

					case TOTAL_WAIT_TIME:
						// more fairness, lower throughput
						// this will work different than ARRIVAL_TIME at undersupply -> will reduce unfairness and
						// throughput
						return pickupBeginTime - reqEntry.destination.getEarliestStartTime();

					default:
						throw new IllegalStateException();
				}
			}
		};
	}

	private Mode getCurrentMode(AssignmentRequestData rData, VehicleData vData) {
		if (params.getMode() != Mode.DSE) {
			return params.getMode();
		} else {
			return rData.getUrgentReqCount() > vData.getIdleCount() ? Mode.PICKUP_TIME : // undersupply
					Mode.ARRIVAL_TIME; // oversupply
		}
	}

	private double calcPickupBeginTime(VehicleData.Entry departure, DestEntry<TaxiRequest> reqEntry,
			PathData pathData) {
		double travelTime = pathData == null ? //
				params.getNullPathCost() : // no path (too far away)
				pathData.getTravelTime();
		return Math.max(reqEntry.destination.getEarliestStartTime(), departure.time + travelTime);
	}
}
