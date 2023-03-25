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

import static org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;

import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;

/**
 * @author michalm
 */
public class DefaultInsertionCostCalculator implements InsertionCostCalculator {
	private final CostCalculationStrategy costCalculationStrategy;
	private final DrtConfigGroup drtConfigGroup;

	public DefaultInsertionCostCalculator(CostCalculationStrategy costCalculationStrategy,
										  DrtConfigGroup drtConfigGroup) {
		this.costCalculationStrategy = costCalculationStrategy;
		this.drtConfigGroup = drtConfigGroup;
	}

	/**
	 * As the main goal is to minimise bus operation time, this method calculates how much longer the bus will operate
	 * after insertion. By returning INFEASIBLE_SOLUTION_COST, the insertion is considered infeasible
	 * <p>
	 * The insertion is invalid if some maxTravel/Wait constraints for the already scheduled requests are not fulfilled.
	 * This is denoted by returning INFEASIBLE_SOLUTION_COST.
	 * <p>
	 *
	 * @param drtRequest the request
	 * @param insertion  the insertion to be considered here
	 * @return cost of insertion (INFEASIBLE_SOLUTION_COST represents an infeasible insertion)
	 */
	@Override
	public double calculate(DrtRequest drtRequest, Insertion insertion, DetourTimeInfo detourTimeInfo) {
		var vEntry = insertion.vehicleEntry;

		if (vEntry.getSlackTime(insertion.pickup.index) < detourTimeInfo.pickupDetourInfo.pickupTimeLoss
				|| vEntry.getSlackTime(insertion.dropoff.index) < detourTimeInfo.getTotalTimeLoss()) {
			return INFEASIBLE_SOLUTION_COST;
		}

		// all stops after the new (potential) pickup but before the new dropoff
		// are delayed by pickupDetourTimeLoss
		double detour = detourTimeInfo.pickupDetourInfo.pickupTimeLoss;
		for (int s = insertion.pickup.index; s < vEntry.stops.size(); s++) {
			Waypoint.Stop stop = vEntry.stops.get(s);
			// passengers are being dropped off == may be close to arrival
			if(!stop.task.getDropoffRequests().isEmpty()) {
				double nextArrival = stop.getArrivalTime();
				double departureTime = insertion.vehicleEntry.start.getDepartureTime();
				//arrival is very soon
				if (nextArrival - departureTime < drtConfigGroup.allowDetourBeforeArrivalThreshold &&
						detour > 0) {
					return INFEASIBLE_SOLUTION_COST;
				}
			}
			if(s == insertion.dropoff.index) {
				// all stops after the new (potential) dropoff are delayed by totalTimeLoss
				detour = detourTimeInfo.getTotalTimeLoss();
			}
		}
		return costCalculationStrategy.calcCost(drtRequest, insertion, detourTimeInfo);
	}
}
