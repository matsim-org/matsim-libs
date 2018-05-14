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
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtTask;
import org.matsim.contrib.drt.schedule.DrtTask.DrtTaskType;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
public class InsertionCostCalculator {
	public static final double INFEASIBLE_SOLUTION_COST = Double.MAX_VALUE;

	private final double stopDuration;
	private final MobsimTimer timer;

	public InsertionCostCalculator(DrtConfigGroup drtConfig, MobsimTimer timer) {
		this(drtConfig.getStopDuration(), timer);
	}

	public InsertionCostCalculator(double stopDuration, MobsimTimer timer) {
		this.stopDuration = stopDuration;
		this.timer = timer;
	}

	// the main goal - minimise bus operation time
	// ==> calculates how much longer the bus will operate after insertion
	//
	// the insertion is invalid if some maxTravel/Wait constraints are not fulfilled
	// ==> checks if all the constraints are satisfied for all passengers/requests ==> if not ==>
	// INFEASIBLE_SOLUTION_COST is returned
	public double calculate(DrtRequest drtRequest, VehicleData.Entry vEntry, InsertionWithDetourTimes insertion) {
		double pickupDetourTimeLoss = calculatePickupDetourTimeLoss(drtRequest, vEntry, insertion);
		double dropoffDetourTimeLoss = calculateDropoffDetourTimeLoss(drtRequest, vEntry, insertion);

		// this is what we want to minimise
		double totalTimeLoss = pickupDetourTimeLoss + dropoffDetourTimeLoss;

		boolean constraintsSatisfied = areConstraintsSatisfied(drtRequest, vEntry, insertion, pickupDetourTimeLoss,
				totalTimeLoss);
		return constraintsSatisfied ? totalTimeLoss : INFEASIBLE_SOLUTION_COST;
	}

	private double calculatePickupDetourTimeLoss(DrtRequest drtRequest, VehicleData.Entry vEntry,
			InsertionWithDetourTimes insertion) {
		final int pickupIdx = insertion.getPickupIdx();
		final int dropoffIdx = insertion.getDropoffIdx();

		// 'no detour' is also possible now for pickupIdx==0 if the currentTask is STOP
		boolean ongoingStopTask = pickupIdx == 0
				&& ((DrtTask)vEntry.vehicle.getSchedule().getCurrentTask()).getDrtTaskType() == DrtTaskType.STOP;

		if ((ongoingStopTask && drtRequest.getFromLink() == vEntry.start.link) //
				|| (pickupIdx > 0 //
						&& drtRequest.getFromLink() == vEntry.stops.get(pickupIdx - 1).task.getLink())) {
			if (pickupIdx != dropoffIdx) {// not: PICKUP->DROPOFF
				return 0;// no detour
			}

			// PICKUP->DROPOFF
			// no extra drive to pickup and stop (==> toPickupTT == 0 and stopDuration == 0)
			double fromPickupTT = insertion.getTimeFromPickup();
			double replacedDriveTT = calculateReplacedDriveDuration(vEntry, pickupIdx);
			return fromPickupTT - replacedDriveTT;
		}

		double toPickupTT = insertion.getTimeToPickup();
		double fromPickupTT = insertion.getTimeFromPickup();
		double replacedDriveTT = pickupIdx == dropoffIdx // PICKUP->DROPOFF ?
				? 0 // no drive following the pickup is replaced (only the one following the dropoff)
				: calculateReplacedDriveDuration(vEntry, pickupIdx);
		return toPickupTT + stopDuration + fromPickupTT - replacedDriveTT;
	}

	private double calculateDropoffDetourTimeLoss(DrtRequest drtRequest, VehicleData.Entry vEntry,
			InsertionWithDetourTimes insertion) {
		final int pickupIdx = insertion.getPickupIdx();
		final int dropoffIdx = insertion.getDropoffIdx();

		if (dropoffIdx > 0 && drtRequest.getToLink() == vEntry.stops.get(dropoffIdx - 1).task.getLink()) {
			return 0; // no detour
		}

		double toDropoffTT = dropoffIdx == pickupIdx // PICKUP->DROPOFF ?
				? 0 // PICKUP->DROPOFF taken into account as fromPickupTT
				: insertion.getTimeToDropoff();
		double fromDropoffTT = dropoffIdx == vEntry.stops.size() // DROPOFF->STAY ?
				? 0 //
				: insertion.getTimeFromDropoff();
		double replacedDriveTT = dropoffIdx == pickupIdx // PICKUP->DROPOFF ?
				? 0 // replacedDriveTT already taken into account in pickupDetourTimeLoss
				: calculateReplacedDriveDuration(vEntry, dropoffIdx);
		return toDropoffTT + stopDuration + fromDropoffTT - replacedDriveTT;
	}

	private double calculateReplacedDriveDuration(VehicleData.Entry vEntry, int insertionIdx) {
		if (insertionIdx == vEntry.stops.size()) {
			return 0;// end of route - bus would wait there
		}

		double replacedDriveStartTime = getDriveToInsertionStartTime(vEntry, insertionIdx);
		double replacedDriveEndTime = vEntry.stops.get(insertionIdx).task.getBeginTime();
		return replacedDriveEndTime - replacedDriveStartTime;
	}

	private boolean areConstraintsSatisfied(DrtRequest drtRequest, VehicleData.Entry vEntry,
			InsertionWithDetourTimes insertion, double pickupDetourTimeLoss, double totalTimeLoss) {
		final int pickupIdx = insertion.getPickupIdx();
		final int dropoffIdx = insertion.getDropoffIdx();

		// this is what we cannot violate
		for (int s = pickupIdx; s < dropoffIdx; s++) {
			Stop stop = vEntry.stops.get(s);
			// all stops after pickup are delayed by pickupDetourTimeLoss
			if (stop.task.getBeginTime() + pickupDetourTimeLoss > stop.maxArrivalTime //
					|| stop.task.getEndTime() + pickupDetourTimeLoss > stop.maxDepartureTime) {
				return false;
			}
		}

		// this is what we cannot violate
		for (int s = dropoffIdx; s < vEntry.stops.size(); s++) {
			Stop stop = vEntry.stops.get(s);
			// all stops after dropoff are delayed by totalTimeLoss
			if (stop.task.getBeginTime() + totalTimeLoss > stop.maxArrivalTime //
					|| stop.task.getEndTime() + totalTimeLoss > stop.maxDepartureTime) {
				return false;
			}
		}

		// reject solutions when maxWaitTime for the new request is violated
		double driveToPickupStartTime = getDriveToInsertionStartTime(vEntry, pickupIdx);
		double pickupEndTime = driveToPickupStartTime + insertion.getTimeToPickup() + stopDuration;

		if (pickupEndTime > drtRequest.getLatestStartTime()) {
			return false;
		}

		// reject solutions when latestArrivalTime for the new request is violated
		double dropoffStartTime = pickupIdx == dropoffIdx//
				? pickupEndTime + insertion.getTimeFromPickup()//
				: vEntry.stops.get(dropoffIdx - 1).task.getEndTime() + pickupDetourTimeLoss
						+ insertion.getTimeToDropoff();

		if (dropoffStartTime > drtRequest.getLatestArrivalTime()) {
			return false;
		}

		// vehicle's time window cannot be violated
		DrtStayTask lastTask = (DrtStayTask)Schedules.getLastTask(vEntry.vehicle.getSchedule());
		double timeSlack = vEntry.vehicle.getServiceEndTime() - Math.max(lastTask.getBeginTime(), timer.getTimeOfDay());
		if (timeSlack < totalTimeLoss) {
			return false;
		}

		return true;// all constraints satisfied
	}

	private double getDriveToInsertionStartTime(VehicleData.Entry vEntry, int insertionIdx) {
		return (insertionIdx == 0) ? vEntry.start.time : vEntry.stops.get(insertionIdx - 1).task.getEndTime();
	}
}
