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

import org.matsim.contrib.drt.optimizer.StopWaypoint;
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

    class DefaultCostCalculationStrategy implements CostCalculationStrategy {
        @Override
        public double calcCost(DrtRequest request, InsertionGenerator.Insertion insertion,
                               DetourTimeInfo detourTimeInfo) {
            if (request.getConstraints().allowRejection()) {
                return rejectSoftConstraintViolations(request, insertion, detourTimeInfo);
            } else {
                return discourageSoftConstraintViolations(request, insertion, detourTimeInfo);
            }
        }


        private static double rejectSoftConstraintViolations(DrtRequest request, InsertionGenerator.Insertion insertion,
                                                             DetourTimeInfo detourTimeInfo) {
            double totalTimeLoss = detourTimeInfo.getTotalTimeLoss();
            if (detourTimeInfo.pickupDetourInfo.requestPickupTime > request.getLatestStartTime()
                    || detourTimeInfo.dropoffDetourInfo.requestDropoffTime > request.getConstraints().latestArrivalTime()) {
                //no extra time is lost => do not check if the current slack time is long enough (can be even negative)
                return InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;
            }

            // check if the max riding time constraints is violated (with default config, the max ride duration
            // is infinity)
            double rideDuration = detourTimeInfo.dropoffDetourInfo.requestDropoffTime - detourTimeInfo.pickupDetourInfo.requestPickupTime;
            if (rideDuration > request.getConstraints().maxRideDuration()) {
                return InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;
            }

            if (insertion != null) {

                // in case of prebooking, we may have intermediate stay times after pickup
                // insertion that may reduce the effective pickup delay that remains that the
                // dropoff insertion point
                double effectiveDropoffTimeLoss = InsertionDetourTimeCalculator.calculateRemainingPickupTimeLossAtDropoff(
                        insertion, detourTimeInfo.pickupDetourInfo) + detourTimeInfo.dropoffDetourInfo.dropoffTimeLoss;

                double lateDiversionThreshold = request.getConstraints().lateDiversionThreshold();
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

    //XXX try to keep penalties reasonably high to prevent people waiting or travelling for hours
    //XXX however, at the same time prefer max-wait-time to max-travel-time violations
    double MAX_WAIT_TIME_VIOLATION_PENALTY = 1;// 1 second of penalty per 1 second of late departure
    double MAX_TRAVEL_TIME_VIOLATION_PENALTY = 10;// 10 seconds of penalty per 1 second of late arrival
    double MAX_RIDE_TIME_VIOLATION_PENALTY = 10;// 10 seconds of penalty per 1 second of exceeded detour
    double LATE_DIVERSION_VIOLATION_PENALTY = 10;// 10 second of penalty per 1 second of late diversion of onboard requests

    private static double discourageSoftConstraintViolations(DrtRequest request, InsertionGenerator.Insertion insertion,
                                                             DetourTimeInfo detourTimeInfo) {

        double totalTimeLoss = detourTimeInfo.getTotalTimeLoss();
        // (additional time vehicle will operate if insertion is accepted)

        double waitTimeViolation = Math.max(0, detourTimeInfo.pickupDetourInfo.requestPickupTime - request.getLatestStartTime());
        // (if drt vehicle picks up too late) (max wait time (often 600 sec) after submission)

        double travelTimeViolation = Math.max(0, detourTimeInfo.dropoffDetourInfo.requestDropoffTime - request.getConstraints().latestArrivalTime());
        // (if drt vehicle drops off too late) (submission time + alpha * directTravelTime + beta)

        double detourViolation = Math.max(0,
                (detourTimeInfo.dropoffDetourInfo.requestDropoffTime - detourTimeInfo.pickupDetourInfo.requestPickupTime)
                        - request.getConstraints().maxRideDuration()
        );

        double lateDiversionViolation = 0;
        if (insertion != null) {
            // in case of prebooking, we may have intermediate stay times after pickup
            // insertion that may reduce the effective pickup delay that remains that the
            // dropoff insertion point
            double effectiveDropoffTimeLoss = InsertionDetourTimeCalculator.calculateRemainingPickupTimeLossAtDropoff(
                    insertion, detourTimeInfo.pickupDetourInfo) + detourTimeInfo.dropoffDetourInfo.dropoffTimeLoss;

            lateDiversionViolation = lateDiversionViolation(insertion, detourTimeInfo, insertion.vehicleEntry, effectiveDropoffTimeLoss, request.getConstraints().lateDiversionThreshold());
        }

        return MAX_WAIT_TIME_VIOLATION_PENALTY * waitTimeViolation
                + MAX_TRAVEL_TIME_VIOLATION_PENALTY * travelTimeViolation
                + MAX_RIDE_TIME_VIOLATION_PENALTY * detourViolation
                + LATE_DIVERSION_VIOLATION_PENALTY * lateDiversionViolation
                + totalTimeLoss;
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
            StopWaypoint stop = vehicleEntry.stops.get(s);
            if (!stop.getTask().getDropoffRequests().isEmpty()) {
                double remainingRideDuration = stop.getArrivalTime() - vehicleEntry.start.getDepartureTime();
                if (remainingRideDuration < lateDiversionThreshold) {
                    violation += lateDiversionThreshold - remainingRideDuration;
                }
            }
        }
        return violation;
    }
}