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

import static org.matsim.contrib.taxi.optimizer.TaxiOptimizerTests.*;

import java.util.List;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.taxi.optimizer.assignment.TaxiToRequestAssignmentCostProvider.Mode;
import org.matsim.testcases.MatsimTestUtils;

public class AssignmentTaxiOptimizerIT {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	@Ignore // temporarily ignore this test due to problems on the build server
	public void testAssignment_arrivalTime() {
		PreloadedBenchmark benchmark = new PreloadedBenchmark("3.0", "25");
		List<TaxiConfigVariant> variants = createDefaultTaxiConfigVariants(true);
		AssignmentTaxiOptimizerParams params = new AssignmentTaxiOptimizerParams();

		params.mode = Mode.ARRIVAL_TIME;
		params.vehPlanningHorizonOversupply = 99999;
		params.vehPlanningHorizonUndersupply = 99999;
		params.nearestRequestsLimit = 99999;
		params.nearestVehiclesLimit = 99999;
		runBenchmark(variants, params, benchmark, utils.getOutputDirectory());
	}

	@Test
	@Ignore // temporarily ignore this test due to problems on the build server
	public void testAssignment_pickupTime() {
		PreloadedBenchmark benchmark = new PreloadedBenchmark("3.0", "25");
		List<TaxiConfigVariant> variants = createDefaultTaxiConfigVariants(true);
		AssignmentTaxiOptimizerParams params = new AssignmentTaxiOptimizerParams();

		params.mode = Mode.PICKUP_TIME;
		params.vehPlanningHorizonOversupply = 120;
		params.vehPlanningHorizonUndersupply = 30;
		params.nearestRequestsLimit = 10;
		params.nearestVehiclesLimit = 10;
		params.reoptimizationTimeStep = 10;
		runBenchmark(variants, params, benchmark, utils.getOutputDirectory());
	}

	@Test
	@Ignore // temporarily ignore this test due to problems on the build server
	public void testAssignment_dse() {
		PreloadedBenchmark benchmark = new PreloadedBenchmark("3.0", "25");
		List<TaxiConfigVariant> variants = createDefaultTaxiConfigVariants(true);
		AssignmentTaxiOptimizerParams params = new AssignmentTaxiOptimizerParams();

		params.vehPlanningHorizonOversupply = 120;
		params.vehPlanningHorizonUndersupply = 30;
		params.nearestRequestsLimit = 10;
		params.nearestVehiclesLimit = 10;
		params.reoptimizationTimeStep = 10;

		params.mode = Mode.DSE;
		runBenchmark(variants, params, benchmark, utils.getOutputDirectory());

	}

	@Test
	@Ignore // temporarily ignore this test due to problems on the build server
	public void testAssignment_totalWaitTime() {
		PreloadedBenchmark benchmark = new PreloadedBenchmark("3.0", "25");
		List<TaxiConfigVariant> variants = createDefaultTaxiConfigVariants(true);
		AssignmentTaxiOptimizerParams params = new AssignmentTaxiOptimizerParams();

		params.vehPlanningHorizonOversupply = 120;
		params.vehPlanningHorizonUndersupply = 30;
		params.nearestRequestsLimit = 10;
		params.nearestVehiclesLimit = 10;
		params.reoptimizationTimeStep = 10;

		params.mode = Mode.TOTAL_WAIT_TIME;
		params.nullPathCost = 300;
		runBenchmark(variants, params, benchmark, utils.getOutputDirectory());
	}
}
