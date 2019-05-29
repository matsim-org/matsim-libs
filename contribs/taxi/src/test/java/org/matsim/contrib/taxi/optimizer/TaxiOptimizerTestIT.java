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

package org.matsim.contrib.taxi.optimizer;

import static org.matsim.contrib.taxi.optimizer.TaxiOptimizerTests.createDefaultTaxiConfigVariants;
import static org.matsim.contrib.taxi.optimizer.TaxiOptimizerTests.runBenchmark;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerTests.PreloadedBenchmark;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerTests.TaxiConfigVariant;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.assignment.TaxiToRequestAssignmentCostProvider.Mode;
import org.matsim.testcases.MatsimTestUtils;

public class TaxiOptimizerTestIT {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testAssignment() {
		PreloadedBenchmark benchmark = new PreloadedBenchmark("3.0", "25");
		List<TaxiConfigVariant> variants = createDefaultTaxiConfigVariants(true);
		AssignmentTaxiOptimizerParams params = new AssignmentTaxiOptimizerParams();

		params.setMode(Mode.ARRIVAL_TIME);
		params.setVehPlanningHorizonOversupply(99999);
		params.setVehPlanningHorizonUndersupply(99999);
		params.setNearestRequestsLimit(99999);
		params.setNearestVehiclesLimit(99999);
		runBenchmark(variants, params, benchmark, utils.getOutputDirectory() + "_A");

		params.setMode(Mode.PICKUP_TIME);
		params.setVehPlanningHorizonOversupply(120);
		params.setVehPlanningHorizonUndersupply(30);
		params.setNearestRequestsLimit(10);
		params.setNearestVehiclesLimit(10);
		params.setReoptimizationTimeStep(10);
		runBenchmark(variants, params, benchmark, utils.getOutputDirectory() + "_B");

		params.setMode(Mode.TOTAL_WAIT_TIME);
		params.setNullPathCost(300);
		runBenchmark(variants, params, benchmark, utils.getOutputDirectory() + "_C");
	}
}
