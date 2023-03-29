/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.optimizer.insertion;

import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.passenger.DrtRequest;

/**
 * @author Michal Maciejewski (michalm)
 */
public interface CostCalculationStrategy {
	/**
	 * @param request
	 * @param insertion
	 * @param detourTimeInfo
	 * @return the cost of insertion, INFEASIBLE_SOLUTION_COST if insertion is not feasible
	 */
	double calcCost(DrtRequest request, InsertionGenerator.Insertion insertion, DetourTimeInfo detourTimeInfo);

	class RejectSoftConstraintViolations implements CostCalculationStrategy {
		@Override
		public double calcCost(DrtRequest request, InsertionGenerator.Insertion insertion,
				DetourTimeInfo detourTimeInfo) {
			double totalTimeLoss = detourTimeInfo.getTotalTimeLoss();
			if (detourTimeInfo.pickupDetourInfo.departureTime > request.getLatestStartTime()
					|| detourTimeInfo.dropoffDetourInfo.arrivalTime > request.getLatestArrivalTime()) {
				//no extra time is lost => do not check if the current slack time is long enough (can be even negative)
				return InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;
			}

			return totalTimeLoss;
		}
	}

	class DiscourageSoftConstraintViolations implements CostCalculationStrategy {
		//XXX try to keep penalties reasonably high to prevent people waiting or travelling for hours
		//XXX however, at the same time prefer max-wait-time to max-travel-time violations
		static final double MAX_WAIT_TIME_VIOLATION_PENALTY = 1;// 1 second of penalty per 1 second of late departure
		static final double MAX_TRAVEL_TIME_VIOLATION_PENALTY = 10;// 10 seconds of penalty per 1 second of late arrival

		@Override
		public double calcCost(DrtRequest request, InsertionGenerator.Insertion insertion,
				DetourTimeInfo detourTimeInfo) {

			double totalTimeLoss = detourTimeInfo.getTotalTimeLoss();
			// (additional time vehicle will operate if insertion is accepted)

			double waitTimeViolation = Math.max(0, detourTimeInfo.pickupDetourInfo.departureTime - request.getLatestStartTime());
			// (if drt vehicle picks up too late) (max wait time (often 600 sec) after submission)

			double travelTimeViolation = Math.max(0, detourTimeInfo.dropoffDetourInfo.arrivalTime - request.getLatestArrivalTime());
			// (if drt vehicle drops off too late) (submission time + alpha * directTravelTime + beta)

			return MAX_WAIT_TIME_VIOLATION_PENALTY * waitTimeViolation
					+ MAX_TRAVEL_TIME_VIOLATION_PENALTY * travelTimeViolation
					+ totalTimeLoss;
		}
	}
}
