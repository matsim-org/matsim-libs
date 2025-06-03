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

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
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
			if (detourTimeInfo.pickupDetourInfo.requestPickupTime > request.getLatestStartTime()
					|| detourTimeInfo.dropoffDetourInfo.requestDropoffTime > request.getLatestArrivalTime()) {
				//no extra time is lost => do not check if the current slack time is long enough (can be even negative)
				return InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;
			}

			// check if the max riding time constraints is violated (with default config, the max ride duration
			// is infinity)
			double rideDuration = detourTimeInfo.dropoffDetourInfo.requestDropoffTime - detourTimeInfo.pickupDetourInfo.requestPickupTime;
			if (rideDuration > request.getMaxRideDuration()) {
				return InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;
			}

			if(insertion != null) {

				// in case of prebooking, we may have intermediate stay times after pickup
				// insertion that may reduce the effective pickup delay that remains that the
				// dropoff insertion point
				double effectiveDropoffTimeLoss = InsertionDetourTimeCalculator.calculateRemainingPickupTimeLossAtDropoff(
						insertion, detourTimeInfo.pickupDetourInfo) + detourTimeInfo.dropoffDetourInfo.dropoffTimeLoss;

				double lateDiversionThreshold = request.getLateDiversionThreshold();
				if (lateDiversionThreshold > 0) {
					if (insertion.vehicleEntry.stops != null && !insertion.vehicleEntry.stops.isEmpty()) {
						if (lateDiversionViolation(insertion, detourTimeInfo, insertion.vehicleEntry, effectiveDropoffTimeLoss, lateDiversionThreshold) > 0) {
							return InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;
						}
					}
				}
			}

			return totalTimeLoss;
		}
	}

	class DiscourageSoftConstraintViolations implements CostCalculationStrategy {
		//XXX try to keep penalties reasonably high to prevent people waiting or travelling for hours
		//XXX however, at the same time prefer max-wait-time to max-travel-time violations
		static final double MAX_WAIT_TIME_VIOLATION_PENALTY = 1;// 1 second of penalty per 1 second of late departure
		static final double MAX_TRAVEL_TIME_VIOLATION_PENALTY = 10;// 10 seconds of penalty per 1 second of late arrival
		static final double MAX_RIDE_TIME_VIOLATION_PENALTY = 10;// 10 seconds of penalty per 1 second of exceeded detour
		static final double LATE_DIVERSION_VIOLATION_PENALTY = 10;// 1 second of penalty per 1 second of late diversion of onboard requests

		@Override
		public double calcCost(DrtRequest request, InsertionGenerator.Insertion insertion,
				DetourTimeInfo detourTimeInfo) {

			double totalTimeLoss = detourTimeInfo.getTotalTimeLoss();
			// (additional time vehicle will operate if insertion is accepted)

			double waitTimeViolation = Math.max(0, detourTimeInfo.pickupDetourInfo.requestPickupTime - request.getLatestStartTime());
			// (if drt vehicle picks up too late) (max wait time (often 600 sec) after submission)

			double travelTimeViolation = Math.max(0, detourTimeInfo.dropoffDetourInfo.requestDropoffTime - request.getLatestArrivalTime());
			// (if drt vehicle drops off too late) (submission time + alpha * directTravelTime + beta)

			double detourViolation = Math.max(0, (detourTimeInfo.dropoffDetourInfo.requestDropoffTime - detourTimeInfo.pickupDetourInfo.requestPickupTime) - request.getMaxRideDuration());

			double lateDiversionViolation = 0;
			if(insertion != null) {
				// in case of prebooking, we may have intermediate stay times after pickup
				// insertion that may reduce the effective pickup delay that remains that the
				// dropoff insertion point
				double effectiveDropoffTimeLoss = InsertionDetourTimeCalculator.calculateRemainingPickupTimeLossAtDropoff(
						insertion, detourTimeInfo.pickupDetourInfo) + detourTimeInfo.dropoffDetourInfo.dropoffTimeLoss;

                lateDiversionViolation = lateDiversionViolation(insertion, detourTimeInfo, insertion.vehicleEntry, effectiveDropoffTimeLoss, request.getLateDiversionThreshold());
            }

            return MAX_WAIT_TIME_VIOLATION_PENALTY * waitTimeViolation
					+ MAX_TRAVEL_TIME_VIOLATION_PENALTY * travelTimeViolation
					+ MAX_RIDE_TIME_VIOLATION_PENALTY * detourViolation
					+ MAX_RIDE_TIME_VIOLATION_PENALTY * lateDiversionViolation
					+ totalTimeLoss;
		}
	}

    private static double lateDiversionViolation(InsertionGenerator.Insertion insertion, DetourTimeInfo detourTimeInfo,
                                                 VehicleEntry vEntry, double effectiveDropoffTimeLoss, double lateDiversionThreshold) {
        double violation = 0;
        if (detourTimeInfo.pickupDetourInfo.pickupTimeLoss > 0) {
            violation += lateDiversionViolationBetweenStopIndices(vEntry, insertion.pickup.index, insertion.dropoff.index, lateDiversionThreshold);
        }
        if (effectiveDropoffTimeLoss > 0) {
            violation += lateDiversionViolationBetweenStopIndices(vEntry, insertion.dropoff.index, vEntry.stops.size(), lateDiversionThreshold);
        }
        return violation;
    }

	private static double lateDiversionViolationBetweenStopIndices(VehicleEntry vehicleEntry, int start, int end, double lateDiversionThreshold) {
		double violation = 0;
		for (int s = start; s < end; s++) {
			Waypoint.Stop stop = vehicleEntry.stops.get(s);
			if (!stop.task.getDropoffRequests().isEmpty()) {
				double remainingRideDuration = stop.getArrivalTime() - vehicleEntry.start.getDepartureTime();
				if (remainingRideDuration < lateDiversionThreshold) {
					violation += lateDiversionThreshold - remainingRideDuration;
				}
			}
		}
		return violation;
	}
}
