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

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.core.mobsim.framework.MobsimTimer;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author michalm
 */
public class DefaultInsertionCostCalculator<D> implements InsertionCostCalculator<D> {

	public static InsertionCostCalculatorFactory createFactory(DrtConfigGroup drtCfg, MobsimTimer timer,
			CostCalculationStrategy costCalculationStrategy) {
		return new InsertionCostCalculatorFactory() {
			@Override
			public <D> InsertionCostCalculator<D> create(ToDoubleFunction<D> detourTime,
					DetourTimeEstimator replacedDriveTimeEstimator) {
				return new DefaultInsertionCostCalculator<>(drtCfg, timer, costCalculationStrategy, detourTime,
						replacedDriveTimeEstimator);
			}
		};
	}

	private final DoubleSupplier timeOfDay;
	private final CostCalculationStrategy costCalculationStrategy;
	private final InsertionDetourTimeCalculator<D> detourTimeCalculator;

	public DefaultInsertionCostCalculator(DrtConfigGroup drtConfig, MobsimTimer timer,
			CostCalculationStrategy costCalculationStrategy, ToDoubleFunction<D> detourTime,
			@Nullable DetourTimeEstimator replacedDriveTimeEstimator) {
		this(timer::getTimeOfDay, costCalculationStrategy,
				new InsertionDetourTimeCalculator<>(drtConfig.getStopDuration(), detourTime,
						replacedDriveTimeEstimator));
	}

	@VisibleForTesting
	DefaultInsertionCostCalculator(DoubleSupplier timeOfDay, CostCalculationStrategy costCalculationStrategy,
			InsertionDetourTimeCalculator<D> detourTimeCalculator) {
		this.timeOfDay = timeOfDay;
		this.costCalculationStrategy = costCalculationStrategy;
		this.detourTimeCalculator = detourTimeCalculator;
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
	public double calculate(DrtRequest drtRequest, InsertionWithDetourData<D> insertion) {
		//TODO precompute time slacks for each stop to filter out even more infeasible insertions ???????????

		var detourTimeInfo = detourTimeCalculator.calculateDetourTimeInfo(insertion);

		if (!checkTimeConstraintsForScheduledRequests(insertion.getInsertion(), detourTimeInfo.pickupTimeLoss,
				detourTimeInfo.getTotalTimeLoss())) {
			return INFEASIBLE_SOLUTION_COST;
		}

		double vehicleSlackTime = calcVehicleSlackTime(insertion.getVehicleEntry(), timeOfDay.getAsDouble());
		return costCalculationStrategy.calcCost(drtRequest, insertion.getInsertion(), vehicleSlackTime, detourTimeInfo);
	}

	static boolean checkTimeConstraintsForScheduledRequests(InsertionGenerator.Insertion insertion,
			double pickupDetourTimeLoss, double totalTimeLoss) {
		VehicleEntry vEntry = insertion.vehicleEntry;
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

	//we assume slack time is never negative
	static double calcVehicleSlackTime(VehicleEntry vEntry, double now) {
		DrtStayTask lastTask = (DrtStayTask)Schedules.getLastTask(vEntry.vehicle.getSchedule());
		//if the last task is started, take 'now', otherwise take the planned begin time
		double availableFromTime = Math.max(lastTask.getBeginTime(), now);
		//for an already delayed vehicle, assume slack is 0 (instead of a negative number)
		return Math.max(0, vEntry.vehicle.getServiceEndTime() - availableFromTime);
	}
}
