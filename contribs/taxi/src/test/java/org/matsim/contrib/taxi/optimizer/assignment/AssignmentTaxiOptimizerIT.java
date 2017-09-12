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

import java.util.*;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider.OptimizerType;
import org.matsim.contrib.taxi.optimizer.assignment.TaxiToRequestAssignmentCostProvider.Mode;
import org.matsim.testcases.MatsimTestUtils;

public class AssignmentTaxiOptimizerIT {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testAssignment() {
		PreloadedBenchmark benchmark = new PreloadedBenchmark("3.0", "25");

		List<TaxiConfigVariant> variants = createDefaultTaxiConfigVariants(true);
		Map<String, String> params = createAbstractOptimParams(OptimizerType.ASSIGNMENT);

		params.put(AssignmentTaxiOptimizerParams.MODE, Mode.DSE.name());
		params.put(AssignmentTaxiOptimizerParams.VEH_PLANNING_HORIZON_OVERSUPPLY, 99999 + "");
		params.put(AssignmentTaxiOptimizerParams.VEH_PLANNING_HORIZON_UNDERSUPPLY, 99999 + "");
		params.put(AssignmentTaxiOptimizerParams.NEAREST_REQUESTS_LIMIT, 99999 + "");
		params.put(AssignmentTaxiOptimizerParams.NEAREST_VEHICLES_LIMIT, 99999 + "");
		runBenchmark(variants, params, benchmark, utils.getOutputDirectory() + "_A");

		params.put(AssignmentTaxiOptimizerParams.MODE, Mode.DSE.name());
		params.put(AssignmentTaxiOptimizerParams.VEH_PLANNING_HORIZON_OVERSUPPLY, 120 + "");
		params.put(AssignmentTaxiOptimizerParams.VEH_PLANNING_HORIZON_UNDERSUPPLY, 30 + "");
		params.put(AssignmentTaxiOptimizerParams.NEAREST_REQUESTS_LIMIT, 10 + "");
		params.put(AssignmentTaxiOptimizerParams.NEAREST_VEHICLES_LIMIT, 10 + "");
		params.put(AbstractTaxiOptimizerParams.REOPTIMIZATION_TIME_STEP, 10 + "");
		runBenchmark(variants, params, benchmark, utils.getOutputDirectory() + "_B");
	}
}
