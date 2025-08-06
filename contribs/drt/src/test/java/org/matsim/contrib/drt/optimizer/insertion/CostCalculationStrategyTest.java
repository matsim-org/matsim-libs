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

import org.junit.jupiter.api.Test;
import org.matsim.contrib.drt.optimizer.constraints.DrtRouteConstraints;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.passenger.DrtRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.contrib.drt.optimizer.insertion.CostCalculationStrategy.MAX_TRAVEL_TIME_VIOLATION_PENALTY;
import static org.matsim.contrib.drt.optimizer.insertion.CostCalculationStrategy.MAX_WAIT_TIME_VIOLATION_PENALTY;
import static org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;
import static org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DropoffDetourInfo;
import static org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.PickupDetourInfo;

/**
 * @author Michal Maciejewski (michalm)
 */
public class CostCalculationStrategyTest {
	@Test
	void RejectSoftConstraintViolations_tooLongWaitTime() {
		assertRejectSoftConstraintViolations(10, 9999,
				new DetourTimeInfo(new PickupDetourInfo(11, 11, 0), new DropoffDetourInfo(22, 22, 0)),
				INFEASIBLE_SOLUTION_COST);
	}

	@Test
	void RejectSoftConstraintViolations_tooLongTravelTime() {
		assertRejectSoftConstraintViolations(9999, 10,
				new DetourTimeInfo(new PickupDetourInfo(0, 0, 0), new DropoffDetourInfo(11, 11, 0)), INFEASIBLE_SOLUTION_COST);
	}

	@Test
	void RejectSoftConstraintViolations_allConstraintSatisfied() {
		assertRejectSoftConstraintViolations(9999, 9999,
				new DetourTimeInfo(new PickupDetourInfo(11, 11, 33), new DropoffDetourInfo(22, 22, 44)), 33 + 44);
	}

	private void assertRejectSoftConstraintViolations(double latestStartTime, double latestArrivalTime,
			DetourTimeInfo detourTimeInfo, double expectedCost) {
		var drtRequest = DrtRequest.newBuilder()
				.constraints(
						new DrtRouteConstraints(
								Double.POSITIVE_INFINITY,
								latestStartTime,
								latestArrivalTime,
								Double.POSITIVE_INFINITY,
								Double.POSITIVE_INFINITY,
								Double.POSITIVE_INFINITY,
								true
						)
				)
				.build();
		assertThat(new CostCalculationStrategy.DefaultCostCalculationStrategy().calcCost(drtRequest, null,
				detourTimeInfo)).isEqualTo(expectedCost);
	}

	@Test
	void DiscourageSoftConstraintViolations_tooLongWaitTime() {
		assertDiscourageSoftConstraintViolations(10, 9999,
				new DetourTimeInfo(new PickupDetourInfo(11, 11, 0), new DropoffDetourInfo(22, 22, 0)),
				MAX_WAIT_TIME_VIOLATION_PENALTY);
	}

	@Test
	void DiscourageSoftConstraintViolations_tooLongTravelTime() {
		assertDiscourageSoftConstraintViolations(9999, 10,
				new DetourTimeInfo(new PickupDetourInfo(0, 0, 0), new DropoffDetourInfo(11, 11, 0)),
				MAX_TRAVEL_TIME_VIOLATION_PENALTY);
	}

	@Test
	void DiscourageSoftConstraintViolations_allConstraintSatisfied() {
		assertDiscourageSoftConstraintViolations(9999, 9999,
				new DetourTimeInfo(new PickupDetourInfo(11, 11, 33), new DropoffDetourInfo(22, 22, 44)), 33 + 44);
	}

	private void assertDiscourageSoftConstraintViolations(double latestStartTime, double latestArrivalTime,
			DetourTimeInfo detourTimeInfo, double expectedCost) {
		var drtRequest = DrtRequest.newBuilder()
				.constraints(
						new DrtRouteConstraints(
								Double.POSITIVE_INFINITY,
								latestStartTime,
								latestArrivalTime,
								Double.POSITIVE_INFINITY,
								Double.POSITIVE_INFINITY,
								Double.POSITIVE_INFINITY,
								false
						)
				)
				.build();
		assertThat(new CostCalculationStrategy.DefaultCostCalculationStrategy().calcCost(drtRequest, null,
				detourTimeInfo)).isEqualTo(expectedCost);
	}
}
