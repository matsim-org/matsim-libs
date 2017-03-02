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

import static org.matsim.contrib.taxi.optimizer.TaxiOptimizerTests.*;

import java.util.*;

import org.junit.Test;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider.OptimizerType;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.assignment.TaxiToRequestAssignmentCostProvider.Mode;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizer.Goal;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;

public class TaxiOptimizerTestIT {
	@Test
	public void testRuleBased() {
		PreloadedBenchmark benchmark = new PreloadedBenchmark("3.0", "25");

		List<TaxiConfigVariant> variants = createDefaultTaxiConfigVariants(false);
		Map<String, String> params = createAbstractOptimParams(OptimizerType.RULE_BASED);

		params.put(RuleBasedTaxiOptimizerParams.GOAL, Goal.MIN_PICKUP_TIME.name());
		params.put(RuleBasedTaxiOptimizerParams.NEAREST_REQUESTS_LIMIT, 1 + "");
		params.put(RuleBasedTaxiOptimizerParams.NEAREST_VEHICLES_LIMIT, 1 + "");
		params.put(RuleBasedTaxiOptimizerParams.CELL_SIZE, 100 + "");
		runBenchmark(variants, params, benchmark);
	}

	@Test
	public void testAssignment() {
		PreloadedBenchmark benchmark = new PreloadedBenchmark("3.0", "25");

		List<TaxiConfigVariant> variants = createDefaultTaxiConfigVariants(true);
		Map<String, String> params = createAbstractOptimParams(OptimizerType.ASSIGNMENT);

		params.put(AssignmentTaxiOptimizerParams.MODE, Mode.ARRIVAL_TIME.name());
		params.put(AssignmentTaxiOptimizerParams.VEH_PLANNING_HORIZON_OVERSUPPLY, 99999 + "");
		params.put(AssignmentTaxiOptimizerParams.VEH_PLANNING_HORIZON_UNDERSUPPLY, 99999 + "");
		params.put(AssignmentTaxiOptimizerParams.NEAREST_REQUESTS_LIMIT, 99999 + "");
		params.put(AssignmentTaxiOptimizerParams.NEAREST_VEHICLES_LIMIT, 99999 + "");
		runBenchmark(variants, params, benchmark);

		params.put(AssignmentTaxiOptimizerParams.MODE, Mode.PICKUP_TIME.name());
		params.put(AssignmentTaxiOptimizerParams.VEH_PLANNING_HORIZON_OVERSUPPLY, 120 + "");
		params.put(AssignmentTaxiOptimizerParams.VEH_PLANNING_HORIZON_UNDERSUPPLY, 30 + "");
		params.put(AssignmentTaxiOptimizerParams.NEAREST_REQUESTS_LIMIT, 10 + "");
		params.put(AssignmentTaxiOptimizerParams.NEAREST_VEHICLES_LIMIT, 10 + "");
		params.put(AbstractTaxiOptimizerParams.REOPTIMIZATION_TIME_STEP, 10 + "");
		runBenchmark(variants, params, benchmark);

		params.put(AssignmentTaxiOptimizerParams.MODE, Mode.TOTAL_WAIT_TIME.name());
		params.put(AssignmentTaxiOptimizerParams.NULL_PATH_COST, 300 + "");
		runBenchmark(variants, params, benchmark);
	}
}
