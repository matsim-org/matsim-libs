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

import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.passenger.DrtRequest;

/**
 * @author michalm
 */
public class DefaultInsertionCostCalculator implements InsertionCostCalculator {
	private final CostCalculationStrategy costCalculationStrategy;

	public DefaultInsertionCostCalculator(CostCalculationStrategy costCalculationStrategy) {
		this.costCalculationStrategy = costCalculationStrategy;
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
		// check if the max riding time constraints is violated (with default config, the max ride duration
		// is infinity)
		if (violatesMaxRideDuration(drtRequest, detourTimeInfo)) {
			return INFEASIBLE_SOLUTION_COST;
		}

		var vEntry = insertion.vehicleEntry;

		// in case of prebooking, we may have intermediate stay times after pickup
		// insertion that may reduce the effective pickup delay that remains that the
		// dropoff insertion point
		double effectiveDropoffTimeLoss = InsertionDetourTimeCalculator.calculateRemainingPickupTimeLossAtDropoff(
				insertion, detourTimeInfo.pickupDetourInfo) + detourTimeInfo.dropoffDetourInfo.dropoffTimeLoss;

		if (vEntry.getSlackTime(insertion.pickup.index) < detourTimeInfo.pickupDetourInfo.pickupTimeLoss
				|| vEntry.getSlackTime(insertion.dropoff.index) < effectiveDropoffTimeLoss) {
			return INFEASIBLE_SOLUTION_COST;
		}

		return costCalculationStrategy.calcCost(drtRequest, insertion, detourTimeInfo);
	}

	private boolean violatesMaxRideDuration(DrtRequest drtRequest, InsertionDetourTimeCalculator.DetourTimeInfo detourTimeInfo) {
		// Check if the max travel time constraint for the newly inserted request is violated
		double rideDuration = detourTimeInfo.dropoffDetourInfo.arrivalTime - detourTimeInfo.pickupDetourInfo.departureTime;
		return drtRequest.getMaxRideDuration() < rideDuration;
	}
}
