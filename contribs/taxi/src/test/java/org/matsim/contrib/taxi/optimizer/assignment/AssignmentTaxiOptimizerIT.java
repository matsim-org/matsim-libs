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
		params.mode = Mode.ARRIVAL_TIME;
		params.vehPlanningHorizonOversupply = 99999;
		params.vehPlanningHorizonUndersupply = 99999;
		params.nearestRequestsLimit = 99999;
		params.nearestVehiclesLimit = 99999;
		runBenchmark(true, params, utils);
	}

	@Test
	void testAssignment_pickupTime() {
		AssignmentTaxiOptimizerParams params = new AssignmentTaxiOptimizerParams();
		params.mode = Mode.PICKUP_TIME;
		params.vehPlanningHorizonOversupply = 120;
		params.vehPlanningHorizonUndersupply = 30;
		params.nearestRequestsLimit = 10;
		params.nearestVehiclesLimit = 10;
		params.reoptimizationTimeStep = 10;
		runBenchmark(true, params, utils);
	}

	@Test
	void testAssignment_dse() {
		AssignmentTaxiOptimizerParams params = new AssignmentTaxiOptimizerParams();
		params.mode = Mode.DSE;
		params.vehPlanningHorizonOversupply = 120;
		params.vehPlanningHorizonUndersupply = 30;
		params.nearestRequestsLimit = 10;
		params.nearestVehiclesLimit = 10;
		params.reoptimizationTimeStep = 10;
		runBenchmark(true, params, utils);
	}

	@Test
	void testAssignment_totalWaitTime() {
		AssignmentTaxiOptimizerParams params = new AssignmentTaxiOptimizerParams();
		params.mode = Mode.TOTAL_WAIT_TIME;
		params.vehPlanningHorizonOversupply = 120;
		params.vehPlanningHorizonUndersupply = 30;
		params.nearestRequestsLimit = 10;
		params.nearestVehiclesLimit = 10;
		params.reoptimizationTimeStep = 10;
		params.nullPathCost = 300;
		runBenchmark(true, params, utils);
	}
}
