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

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.contrib.drt.optimizer.insertion.CostCalculationStrategy.DiscourageSoftConstraintViolations.MAX_TRAVEL_TIME_VIOLATION_PENALTY;
import static org.matsim.contrib.drt.optimizer.insertion.CostCalculationStrategy.DiscourageSoftConstraintViolations.MAX_WAIT_TIME_VIOLATION_PENALTY;
import static org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;

import org.junit.Test;
import org.matsim.contrib.drt.passenger.DrtRequest;

/**
 * @author Michal Maciejewski (michalm)
 */
public class CostCalculationStrategyTest {
	@Test
	public void RejectSoftConstraintViolations_tooLittleSlackTime() {
		assertRejectSoftConstraintViolations(9999, 9999, 10, new InsertionCostCalculator.DetourTimeInfo(0, 0, 5, 5.01),
				INFEASIBLE_SOLUTION_COST);
	}

	@Test
	public void RejectSoftConstraintViolations_negativeSlackTime_butNoTimeLoss() {
		assertRejectSoftConstraintViolations(9999, 9999, -10, new InsertionCostCalculator.DetourTimeInfo(0, 0, 0, 0),
				0);
	}

	@Test
	public void RejectSoftConstraintViolations_tooLongWaitTime() {
		assertRejectSoftConstraintViolations(10, 9999, 9999, new InsertionCostCalculator.DetourTimeInfo(11, 22, 0, 0),
				INFEASIBLE_SOLUTION_COST);
	}

	@Test
	public void RejectSoftConstraintViolations_tooLongTravelTime() {
		assertRejectSoftConstraintViolations(9999, 10, 9999, new InsertionCostCalculator.DetourTimeInfo(0, 11, 0, 0),
				INFEASIBLE_SOLUTION_COST);
	}

	@Test
	public void RejectSoftConstraintViolations_allConstraintSatisfied() {
		assertRejectSoftConstraintViolations(9999, 9999, 9999,
				new InsertionCostCalculator.DetourTimeInfo(11, 22, 33, 44), 33 + 44);
	}

	private void assertRejectSoftConstraintViolations(double latestStartTime, double latestArrivalTime,
			double vehicleSlackTime, InsertionCostCalculator.DetourTimeInfo detourTimeInfo, double expectedCost) {
		var drtRequest = DrtRequest.newBuilder()
				.latestStartTime(latestStartTime)
				.latestArrivalTime(latestArrivalTime)
				.build();
		assertThat(new CostCalculationStrategy.RejectSoftConstraintViolations().calcCost(drtRequest, null,
				vehicleSlackTime, detourTimeInfo)).isEqualTo(expectedCost);
	}

	@Test
	public void DiscourageSoftConstraintViolations_tooLittleSlackTime() {
		assertDiscourageSoftConstraintViolations(9999, 9999, 10,
				new InsertionCostCalculator.DetourTimeInfo(0, 0, 5, 5.01), INFEASIBLE_SOLUTION_COST);
	}

	@Test
	public void DiscourageSoftConstraintViolations_negativeSlackTime_butNoTimeLoss() {
		assertDiscourageSoftConstraintViolations(9999, 9999, -10,
				new InsertionCostCalculator.DetourTimeInfo(0, 0, 0, 0), 0);
	}

	@Test
	public void DiscourageSoftConstraintViolations_tooLongWaitTime() {
		assertDiscourageSoftConstraintViolations(10, 9999, 9999,
				new InsertionCostCalculator.DetourTimeInfo(11, 22, 0, 0), MAX_WAIT_TIME_VIOLATION_PENALTY);
	}

	@Test
	public void DiscourageSoftConstraintViolations_tooLongTravelTime() {
		assertDiscourageSoftConstraintViolations(9999, 10, 9999,
				new InsertionCostCalculator.DetourTimeInfo(0, 11, 0, 0), MAX_TRAVEL_TIME_VIOLATION_PENALTY);
	}

	@Test
	public void DiscourageSoftConstraintViolations_allConstraintSatisfied() {
		assertDiscourageSoftConstraintViolations(9999, 9999, 9999,
				new InsertionCostCalculator.DetourTimeInfo(11, 22, 33, 44), 33 + 44);
	}

	private void assertDiscourageSoftConstraintViolations(double latestStartTime, double latestArrivalTime,
			double vehicleSlackTime, InsertionCostCalculator.DetourTimeInfo detourTimeInfo, double expectedCost) {
		var drtRequest = DrtRequest.newBuilder()
				.latestStartTime(latestStartTime)
				.latestArrivalTime(latestArrivalTime)
				.build();
		assertThat(new CostCalculationStrategy.DiscourageSoftConstraintViolations().calcCost(drtRequest, null,
				vehicleSlackTime, detourTimeInfo)).isEqualTo(expectedCost);
	}
}
