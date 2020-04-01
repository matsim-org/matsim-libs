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

package org.matsim.contrib.taxi.optimizer.rules;

import static org.matsim.contrib.taxi.optimizer.TaxiOptimizerTests.createDefaultTaxiConfigVariants;
import static org.matsim.contrib.taxi.optimizer.TaxiOptimizerTests.runBenchmark;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerTests.PreloadedBenchmark;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerTests.TaxiConfigVariant;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedRequestInserter.Goal;
import org.matsim.testcases.MatsimTestUtils;

public class RuleBasedTaxiOptimizerIT {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRuleBased() {
		PreloadedBenchmark benchmark = new PreloadedBenchmark("3.0", "25");
		List<TaxiConfigVariant> variants = createDefaultTaxiConfigVariants(false);
		RuleBasedTaxiOptimizerParams params = new RuleBasedTaxiOptimizerParams();

		params.setGoal(Goal.DEMAND_SUPPLY_EQUIL);
		params.setNearestRequestsLimit(99999);
		params.setNearestVehiclesLimit(99999);
		params.setCellSize(99999.);
		runBenchmark(variants, params, benchmark, utils.getOutputDirectory() + "_A");

		params.setGoal(Goal.MIN_WAIT_TIME);
		params.setNearestRequestsLimit(10);
		params.setNearestVehiclesLimit(10);
		params.setCellSize(1000.);
		runBenchmark(variants, params, benchmark, utils.getOutputDirectory() + "_B");

		params.setGoal(Goal.MIN_PICKUP_TIME);
		params.setNearestRequestsLimit(1);
		params.setNearestVehiclesLimit(1);
		params.setCellSize(100.);
		runBenchmark(variants, params, benchmark, utils.getOutputDirectory() + "_C");
	}
}
