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

package playground.michalm.drt.optimizer.insertion;

import org.matsim.contrib.dvrp.schedule.Schedules;

import playground.michalm.drt.data.NDrtRequest;
import playground.michalm.drt.optimizer.VehicleData;
import playground.michalm.drt.optimizer.VehicleData.Stop;
import playground.michalm.drt.optimizer.insertion.SingleVehicleInsertionProblem.Insertion;
import playground.michalm.drt.schedule.*;
import playground.michalm.drt.schedule.NDrtTask.NDrtTaskType;

/**
 * @author michalm
 */
public class InsertionCostCalculator {
	private static final double INFEASIBLE_SOLUTION_COST = Double.MAX_VALUE;

	private final double stopDuration;

	public InsertionCostCalculator(double stopDuration) {
		this.stopDuration = stopDuration;
	}

	// the main goal - minimise bus operation time
	// ==> calculates how much longer the bus will operate after insertion
	//
	// the insertion is invalid if some maxTravel/Wait constraints are not fulfilled
	// ==> checks if all the constraints are satisfied for all passengers/requests ==> if not ==>
	// INFEASIBLE_SOLUTION_COST is returned
	public double calculate(NDrtRequest drtRequest, VehicleData.Entry vEntry, Insertion insertion) {
		int i = insertion.pickupIdx;
		int j = insertion.dropoffIdx;

		double pickupDetourDuration = calculatePickupDetourDuration(drtRequest, vEntry, insertion);
		double dropoffDetourDuration = calculateDropoffDetourDuration(drtRequest, vEntry, insertion);

		double pickupDetourTimeLoss = pickupDetourDuration - calculateReplacedDriveDuration(vEntry, i);
		double dropoffDetourTimeLoss = dropoffDetourDuration //
				- (i == j ? 0 : calculateReplacedDriveDuration(vEntry, j));

		// this is what we want to minimise
		double totalTimeLoss = pickupDetourTimeLoss + dropoffDetourTimeLoss;

		return areConstraintsSatisfied(vEntry, insertion, pickupDetourTimeLoss, totalTimeLoss) ? totalTimeLoss
				: INFEASIBLE_SOLUTION_COST;
	}

	private double calculatePickupDetourDuration(NDrtRequest drtRequest, VehicleData.Entry vEntry,
			Insertion insertion) {
		// 'no detour' is also possible now for pickupIdx==0 if the currentTask is STOP
		boolean ongoingStopTask = insertion.pickupIdx == 0
				&& ((NDrtTask)vEntry.vehicle.getSchedule().getCurrentTask()).getDrtTaskType() == NDrtTaskType.STOP;

		if ((ongoingStopTask || insertion.pickupIdx > 0) //
				&& drtRequest.getFromLink() == vEntry.stops.get(insertion.pickupIdx - 1).task.getLink()) {
			return 0;// no detour
		}

		double toPickupTT = insertion.pathToPickup.path.travelTime + insertion.pathToPickup.firstAndLastLinkTT;
		double fromPickupTT = insertion.pathFromPickup.path.travelTime + insertion.pathFromPickup.firstAndLastLinkTT;
		return toPickupTT + stopDuration + fromPickupTT;
	}

	private double calculateDropoffDetourDuration(NDrtRequest drtRequest, VehicleData.Entry vEntry,
			Insertion insertion) {
		if (insertion.dropoffIdx > 0
				&& drtRequest.getToLink() == vEntry.stops.get(insertion.dropoffIdx - 1).task.getLink()) {
			return 0; // no detour
		}

		boolean dropoffImmediatelyAfterPickup = insertion.dropoffIdx == insertion.pickupIdx;
		boolean dropoffAppendedAtTheEnd = insertion.dropoffIdx == vEntry.stops.size();

		double toDropoffTT = dropoffImmediatelyAfterPickup ? 0 // PICKUP->DROPOFF taken into account as fromPickupTT
				: insertion.pathToDropoff.path.travelTime + insertion.pathToDropoff.firstAndLastLinkTT;
		double fromDropoffTT = dropoffAppendedAtTheEnd ? 0 // bus waits there
				: insertion.pathFromDropoff.path.travelTime + insertion.pathFromDropoff.firstAndLastLinkTT;
		return toDropoffTT + stopDuration + fromDropoffTT;
	}

	private double calculateReplacedDriveDuration(VehicleData.Entry vEntry, int insertionIdx) {
		if (insertionIdx == vEntry.stops.size()) {
			return 0;// end of route - bus would wait there
		}

		double replacedDriveStartTime = (insertionIdx == 0) ? vEntry.start.time //
				: vEntry.stops.get(insertionIdx - 1).task.getEndTime();
		double replacedDriveEndTime = vEntry.stops.get(insertionIdx).task.getEndTime();
		return replacedDriveEndTime - replacedDriveStartTime;
	}

	private boolean areConstraintsSatisfied(VehicleData.Entry vEntry, Insertion insertion, double pickupDetourTimeLoss,
			double totalTimeLoss) {
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

		// vehicle's time window cannot be violated
		NDrtStayTask lastTask = (NDrtStayTask)Schedules.getLastTask(vEntry.vehicle.getSchedule());
		double lastTaskDuration = lastTask.getEndTime() - lastTask.getBeginTime();
		return lastTaskDuration >= totalTimeLoss;
	}
}
