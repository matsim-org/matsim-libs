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

import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.VehicleData.Stop;
import org.matsim.contrib.drt.optimizer.insertion.SingleVehicleInsertionProblem.Insertion;
import org.matsim.contrib.drt.schedule.*;
import org.matsim.contrib.drt.schedule.DrtTask.DrtTaskType;
import org.matsim.contrib.dvrp.schedule.Schedules;

/**
 * @author michalm
 */
public class InsertionCostCalculator {
	private static final double INFEASIBLE_SOLUTION_COST = Double.MAX_VALUE;

	private final double stopDuration;
	private final double maxWaitTime;

	public InsertionCostCalculator(double stopDuration, double maxWaitTime) {
		this.stopDuration = stopDuration;
		this.maxWaitTime = maxWaitTime;
	}

	// the main goal - minimise bus operation time
	// ==> calculates how much longer the bus will operate after insertion
	//
	// the insertion is invalid if some maxTravel/Wait constraints are not fulfilled
	// ==> checks if all the constraints are satisfied for all passengers/requests ==> if not ==>
	// INFEASIBLE_SOLUTION_COST is returned
	public double calculate(DrtRequest drtRequest, VehicleData.Entry vEntry, Insertion insertion) {
		double pickupDetourTimeLoss = calculatePickupDetourTimeLoss(drtRequest, vEntry, insertion);
		double dropoffDetourTimeLoss = calculateDropoffDetourTimeLoss(drtRequest, vEntry, insertion);

		// this is what we want to minimise
		double totalTimeLoss = pickupDetourTimeLoss + dropoffDetourTimeLoss;

		boolean constraintsSatisfied = areConstraintsSatisfied(drtRequest, vEntry, insertion, pickupDetourTimeLoss,
				totalTimeLoss);
		return constraintsSatisfied ? totalTimeLoss : INFEASIBLE_SOLUTION_COST;
	}

	private double calculatePickupDetourTimeLoss(DrtRequest drtRequest, VehicleData.Entry vEntry, Insertion insertion) {
		// 'no detour' is also possible now for pickupIdx==0 if the currentTask is STOP
		boolean ongoingStopTask = insertion.pickupIdx == 0
				&& ((DrtTask)vEntry.vehicle.getSchedule().getCurrentTask()).getDrtTaskType() == DrtTaskType.STOP;

		if ((ongoingStopTask && drtRequest.getFromLink() == vEntry.start.link) //
				|| (insertion.pickupIdx > 0 //
						&& drtRequest.getFromLink() == vEntry.stops.get(insertion.pickupIdx - 1).task.getLink())) {
			if (insertion.pickupIdx != insertion.dropoffIdx) {// PICKUP->DROPOFF
				return 0;// no detour
			}

			// no extra drive to pickup and stop (==> toPickupTT == 0 and stopDuration == 0)
			double fromPickupTT = insertion.pathFromPickup.path.travelTime
					+ insertion.pathFromPickup.firstAndLastLinkTT;
			double replacedDriveTT = calculateReplacedDriveDuration(vEntry, insertion.pickupIdx);
			return fromPickupTT - replacedDriveTT;
		}

		double toPickupTT = insertion.pathToPickup.path.travelTime + insertion.pathToPickup.firstAndLastLinkTT;
		double fromPickupTT = insertion.pathFromPickup.path.travelTime + insertion.pathFromPickup.firstAndLastLinkTT;
		double replacedDriveTT = calculateReplacedDriveDuration(vEntry, insertion.pickupIdx);
		return toPickupTT + stopDuration + fromPickupTT - replacedDriveTT;
	}

	private double calculateDropoffDetourTimeLoss(DrtRequest drtRequest, VehicleData.Entry vEntry,
			Insertion insertion) {
		if (insertion.dropoffIdx > 0
				&& drtRequest.getToLink() == vEntry.stops.get(insertion.dropoffIdx - 1).task.getLink()) {
			return 0; // no detour
		}

		double toDropoffTT = insertion.dropoffIdx == insertion.pickupIdx ? // PICKUP->DROPOFF ?
				0 // PICKUP->DROPOFF taken into account as fromPickupTT
				: insertion.pathToDropoff.path.travelTime + insertion.pathToDropoff.firstAndLastLinkTT;
		double fromDropoffTT = insertion.dropoffIdx == vEntry.stops.size() ? // DROPOFF->STAY ?
				0 //
				: insertion.pathFromDropoff.path.travelTime + insertion.pathFromDropoff.firstAndLastLinkTT;
		double replacedDriveTT = insertion.dropoffIdx == insertion.pickupIdx ? // PICKUP->DROPOFF ?
				0 // replacedDriveTT already taken into account in pickupDetourTimeLoss
				: calculateReplacedDriveDuration(vEntry, insertion.dropoffIdx);
		return toDropoffTT + stopDuration + fromDropoffTT - replacedDriveTT;
	}

	private double calculateReplacedDriveDuration(VehicleData.Entry vEntry, int insertionIdx) {
		if (insertionIdx == vEntry.stops.size()) {
			return 0;// end of route - bus would wait there
		}

		double replacedDriveStartTime = (insertionIdx == 0) ? vEntry.start.time //
				: vEntry.stops.get(insertionIdx - 1).task.getEndTime();
		double replacedDriveEndTime = vEntry.stops.get(insertionIdx).task.getBeginTime();
		return replacedDriveEndTime - replacedDriveStartTime;
	}

	private boolean areConstraintsSatisfied(DrtRequest drtRequest, VehicleData.Entry vEntry, Insertion insertion,
			double pickupDetourTimeLoss, double totalTimeLoss) {
		// this is what we cannot violate
		for (int s = insertion.pickupIdx; s < insertion.dropoffIdx; s++) {
			Stop stop = vEntry.stops.get(s);
			// all stops after pickup are delayed by pickupDetourTimeLoss
			if (stop.task.getBeginTime() + pickupDetourTimeLoss > stop.maxArrivalTime //
					|| stop.task.getEndTime() + pickupDetourTimeLoss > stop.maxDepartureTime) {
				return false;
			}
		}

		// this is what we cannot violate
		for (int s = insertion.dropoffIdx; s < vEntry.stops.size(); s++) {
			Stop stop = vEntry.stops.get(s);
			// all stops after dropoff are delayed by totalTimeLoss
			if (stop.task.getBeginTime() + totalTimeLoss > stop.maxArrivalTime //
					|| stop.task.getEndTime() + totalTimeLoss > stop.maxDepartureTime) {
				return false;
			}
		}

		// reject solutions when maxWaitTime for the new request is violated
		double driveToPickupStartTime = (insertion.pickupIdx == 0) ? vEntry.start.time //
				: vEntry.stops.get(insertion.pickupIdx - 1).task.getEndTime();

		double pickupTime = driveToPickupStartTime + insertion.pathToPickup.path.travelTime
				+ insertion.pathToPickup.firstAndLastLinkTT;

		if (pickupTime > drtRequest.getEarliestStartTime() + maxWaitTime) {
			return false;
		}

		// vehicle's time window cannot be violated
		DrtStayTask lastTask = (DrtStayTask)Schedules.getLastTask(vEntry.vehicle.getSchedule());
		double timeSlack = vEntry.vehicle.getServiceEndTime() - lastTask.getBeginTime();
		if (timeSlack < totalTimeLoss) {
			return false;
		}

		return true;// all constraints satisfied
	}
}
