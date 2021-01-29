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

import java.util.function.DoubleSupplier;
import java.util.function.ToDoubleFunction;

import javax.annotation.Nullable;

import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
public class InsertionCostCalculator<D> {
	public static class DetourTimeInfo {
		public final double departureTime;
		public final double arrivalTime;
		public final double pickupTimeLoss;
		public final double dropoffTimeLoss;

		public DetourTimeInfo(double departureTime, double arrivalTime, double pickupTimeLoss, double dropoffTimeLoss) {
			this.departureTime = departureTime;
			this.arrivalTime = arrivalTime;
			this.pickupTimeLoss = pickupTimeLoss;
			this.dropoffTimeLoss = dropoffTimeLoss;
		}

		public double getTotalTimeLoss() {
			return pickupTimeLoss + dropoffTimeLoss;
		}
	}

	public interface CostCalculationStrategy {
		double calcCost(DrtRequest request, InsertionGenerator.Insertion insertion, double vehicleSlackTime,
				DetourTimeInfo detourTimeInfo);
	}

	public static class RejectSoftConstraintViolations implements CostCalculationStrategy {
		@Override
		public double calcCost(DrtRequest request, InsertionGenerator.Insertion insertion, double vehicleSlackTime,
				DetourTimeInfo detourTimeInfo) {
			double totalTimeLoss = detourTimeInfo.getTotalTimeLoss();
			if (totalTimeLoss - vehicleSlackTime > 0
					|| detourTimeInfo.departureTime - request.getLatestStartTime() > 0
					|| detourTimeInfo.arrivalTime - request.getLatestArrivalTime() > 0) {
				return INFEASIBLE_SOLUTION_COST;
			}

			return totalTimeLoss;
		}
	}

	public static class DiscourageSoftConstraintViolations implements CostCalculationStrategy {
		//XXX try to keep penalties reasonably high to prevent people waiting or travelling for hours
		//XXX however, at the same time prefer max-wait-time to max-travel-time violations
		private static final double MAX_WAIT_TIME_VIOLATION_PENALTY = 1;// 1 second of penalty per 1 second of late departure
		private static final double MAX_TRAVEL_TIME_VIOLATION_PENALTY = 10;// 10 seconds of penalty per 1 second of late arrival

		@Override
		public double calcCost(DrtRequest request, InsertionGenerator.Insertion insertion, double vehicleSlackTime,
				DetourTimeInfo detourTimeInfo) {
			double totalTimeLoss = detourTimeInfo.getTotalTimeLoss();
			if (totalTimeLoss - vehicleSlackTime > 0) {
				return INFEASIBLE_SOLUTION_COST;
			}

			double waitTimeViolation = Math.max(0, detourTimeInfo.departureTime - request.getLatestStartTime());
			double travelTimeViolation = Math.max(0, detourTimeInfo.arrivalTime - request.getLatestArrivalTime());
			return MAX_WAIT_TIME_VIOLATION_PENALTY * waitTimeViolation
					+ MAX_TRAVEL_TIME_VIOLATION_PENALTY * travelTimeViolation
					+ totalTimeLoss;
		}
	}

	public static final double INFEASIBLE_SOLUTION_COST = Double.POSITIVE_INFINITY;

	private final DoubleSupplier timeOfDay;
	private final CostCalculationStrategy costCalculationStrategy;
	private final InsertionDetourTimeCalculator<D> detourTimeCalculator;

	public InsertionCostCalculator(DrtConfigGroup drtConfig, MobsimTimer timer,
			CostCalculationStrategy costCalculationStrategy, ToDoubleFunction<D> detourTime,
			@Nullable DetourTimeEstimator replacedDriveTimeEstimator) {
		this(drtConfig.getStopDuration(), timer::getTimeOfDay, costCalculationStrategy, detourTime,
				replacedDriveTimeEstimator);
	}

	public InsertionCostCalculator(double stopDuration, DoubleSupplier timeOfDay,
			CostCalculationStrategy costCalculationStrategy, ToDoubleFunction<D> detourTime,
			@Nullable DetourTimeEstimator replacedDriveTimeEstimator) {
		this.timeOfDay = timeOfDay;
		this.costCalculationStrategy = costCalculationStrategy;

		detourTimeCalculator = new InsertionDetourTimeCalculator<>(stopDuration, detourTime,
				replacedDriveTimeEstimator);
	}

	/**
	 * As the main goal is to minimise bus operation time, this method calculates how much longer the bus will operate
	 * after insertion. By returning a value equal or higher than INFEASIBLE_SOLUTION_COST, the insertion is considered
	 * infeasible
	 * <p>
	 * The insertion is invalid if some maxTravel/Wait constraints for the already scheduled requests are not fulfilled
	 * or the vehicle's time window is violated (hard constraints). This is denoted by returning INFEASIBLE_SOLUTION_COST.
	 * <p>
	 * However, not fulfilling the maxTravel/Time constraints (soft constraints) is penalised using
	 * PenaltyCalculator. If the penalty is at least as high as INFEASIBLE_SOLUTION_COST, the soft
	 * constraint becomes effectively a hard one.
	 *
	 * @param drtRequest the request
	 * @param insertion  the insertion to be considered here, with PickupIdx and DropoffIdx the positions
	 * @return cost of insertion (values higher or equal to INFEASIBLE_SOLUTION_COST represent an infeasible insertion)
	 */
	public double calculate(DrtRequest drtRequest, InsertionWithDetourData<D> insertion) {
		//TODO precompute time slacks for each stop to filter out even more infeasible insertions ???????????

		var detourTimeInfo = detourTimeCalculator.calculateDetourTimeInfo(insertion);
		// the pickupTimeLoss is needed for stops that suffer only that one, while the sum of both will be suffered by
		// the stops after the dropoff stop. kai, nov'18
		// The computation is complicated; presumably, it takes care of this.  kai, nov'18

		// this is what we want to minimise
		if (!checkTimeConstraintsForScheduledRequests(insertion.getInsertion(), detourTimeInfo.pickupTimeLoss,
				detourTimeInfo.getTotalTimeLoss())) {
			return INFEASIBLE_SOLUTION_COST;
		}

		double vehicleSlackTime = calcVehicleSlackTime(insertion.getVehicleEntry(), timeOfDay.getAsDouble());
		return costCalculationStrategy.calcCost(drtRequest, insertion.getInsertion(), vehicleSlackTime, detourTimeInfo);
	}

	static boolean checkTimeConstraintsForScheduledRequests(InsertionGenerator.Insertion insertion,
			double pickupDetourTimeLoss, double totalTimeLoss) {
		VehicleData.Entry vEntry = insertion.vehicleEntry;
		final int pickupIdx = insertion.pickup.index;
		final int dropoffIdx = insertion.dropoff.index;

		// each existing stop has 2 time constraints: latestArrivalTime and latestDepartureTime (see: Waypoint.Stop)
		// we are looking only at the time constraints of the scheduled requests (the new request is checked separately)

		// all stops after the new (potential) pickup but before the new dropoff are delayed by pickupDetourTimeLoss
		// check if this delay satisfies the time constraints at these stops
		for (int s = pickupIdx; s < dropoffIdx; s++) {
			Waypoint.Stop stop = vEntry.stops.get(s);
			if (stop.task.getBeginTime() + pickupDetourTimeLoss > stop.latestArrivalTime
					|| stop.task.getEndTime() + pickupDetourTimeLoss > stop.latestDepartureTime) {
				return false;
			}
		}

		// all stops after the new (potential) dropoff are delayed by totalTimeLoss
		// check if this delay satisfies the time constraints at these stops
		for (int s = dropoffIdx; s < vEntry.stops.size(); s++) {
			Waypoint.Stop stop = vEntry.stops.get(s);
			if (stop.task.getBeginTime() + totalTimeLoss > stop.latestArrivalTime
					|| stop.task.getEndTime() + totalTimeLoss > stop.latestDepartureTime) {
				return false;
			}
		}

		return true; //all time constraints of all stops are satisfied
	}

	static double calcVehicleSlackTime(VehicleData.Entry vEntry, double now) {
		DrtStayTask lastTask = (DrtStayTask)Schedules.getLastTask(vEntry.vehicle.getSchedule());
		return vEntry.vehicle.getServiceEndTime() - Math.max(lastTask.getBeginTime(), now);
	}
}
