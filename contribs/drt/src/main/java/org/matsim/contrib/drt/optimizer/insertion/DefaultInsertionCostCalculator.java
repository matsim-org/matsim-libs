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

import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
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
				return new DefaultInsertionCostCalculator<>(drtCfg, costCalculationStrategy, detourTime,
						replacedDriveTimeEstimator);
			}
		};
	}

	private final CostCalculationStrategy costCalculationStrategy;
	private final InsertionDetourTimeCalculator<D> detourTimeCalculator;

	public DefaultInsertionCostCalculator(DrtConfigGroup drtConfig, CostCalculationStrategy costCalculationStrategy,
			ToDoubleFunction<D> detourTime, @Nullable DetourTimeEstimator replacedDriveTimeEstimator) {
		this(costCalculationStrategy, new InsertionDetourTimeCalculator<>(drtConfig.getStopDuration(), detourTime,
				replacedDriveTimeEstimator));
	}

	@VisibleForTesting
	DefaultInsertionCostCalculator(CostCalculationStrategy costCalculationStrategy,
			InsertionDetourTimeCalculator<D> detourTimeCalculator) {
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

		var insertion1 = insertion.getInsertion();
		var vEntry = insertion1.vehicleEntry;

		if (vEntry.getSlackTime(insertion1.pickup.index) < detourTimeInfo.pickupTimeLoss
				|| vEntry.getSlackTime(insertion1.dropoff.index) < detourTimeInfo.getTotalTimeLoss()) {
			return INFEASIBLE_SOLUTION_COST;
		}

		return costCalculationStrategy.calcCost(drtRequest, insertion1, detourTimeInfo);
	}
}
