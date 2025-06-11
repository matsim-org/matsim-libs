/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer.assignment;

import static org.matsim.contrib.taxi.optimizer.TaxiOptimizerTests.runBenchmark;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.taxi.optimizer.assignment.TaxiToRequestAssignmentCostProvider.Mode;
import org.matsim.testcases.MatsimTestUtils;

public class AssignmentTaxiOptimizerIT {
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testAssignment_arrivalTime() {
		AssignmentTaxiOptimizerParams params = new AssignmentTaxiOptimizerParams();
		params.setMode(Mode.ARRIVAL_TIME);
		params.setVehPlanningHorizonOversupply(99999);
		params.setVehPlanningHorizonUndersupply(99999);
		params.setNearestRequestsLimit(99999);
		params.setNearestVehiclesLimit(99999);
		runBenchmark(true, params, utils);
	}

	@Test
	void testAssignment_pickupTime() {
		AssignmentTaxiOptimizerParams params = new AssignmentTaxiOptimizerParams();
		params.setMode(Mode.PICKUP_TIME);
		params.setVehPlanningHorizonOversupply(120);
		params.setVehPlanningHorizonUndersupply(30);
		params.setNearestRequestsLimit(10);
		params.setNearestVehiclesLimit(10);
		params.setReoptimizationTimeStep(10);
		runBenchmark(true, params, utils);
	}

	@Test
	void testAssignment_dse() {
		AssignmentTaxiOptimizerParams params = new AssignmentTaxiOptimizerParams();
		params.setMode(Mode.DSE);
		params.setVehPlanningHorizonOversupply(120);
		params.setVehPlanningHorizonUndersupply(30);
		params.setNearestRequestsLimit(10);
		params.setNearestVehiclesLimit(10);
		params.setReoptimizationTimeStep(10);
		runBenchmark(true, params, utils);
	}

	@Test
	void testAssignment_totalWaitTime() {
		AssignmentTaxiOptimizerParams params = new AssignmentTaxiOptimizerParams();
		params.setMode(Mode.TOTAL_WAIT_TIME);
		params.setVehPlanningHorizonOversupply(120);
		params.setVehPlanningHorizonUndersupply(30);
		params.setNearestRequestsLimit(10);
		params.setNearestVehiclesLimit(10);
		params.setReoptimizationTimeStep(10);
		params.setNullPathCost(300);
		runBenchmark(true, params, utils);
	}
}
