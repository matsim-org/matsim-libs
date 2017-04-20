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

import static org.matsim.contrib.taxi.optimizer.TaxiOptimizerTests.*;

import java.util.*;

import org.junit.Test;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider.OptimizerType;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizer.Goal;

public class RuleBasedTaxiOptimizerIT {
	@Test
	public void testRuleBased() {
		PreloadedBenchmark benchmark = new PreloadedBenchmark("3.0", "25");

		List<TaxiConfigVariant> variants = createDefaultTaxiConfigVariants(false);
		Map<String, String> params = createAbstractOptimParams(OptimizerType.RULE_BASED);

		params.put(RuleBasedTaxiOptimizerParams.GOAL, Goal.DEMAND_SUPPLY_EQUIL.name());
		params.put(RuleBasedTaxiOptimizerParams.NEAREST_REQUESTS_LIMIT, 99999 + "");
		params.put(RuleBasedTaxiOptimizerParams.NEAREST_VEHICLES_LIMIT, 99999 + "");
		params.put(RuleBasedTaxiOptimizerParams.CELL_SIZE, 99999 + "");
		runBenchmark(variants, params, benchmark);

		params.put(RuleBasedTaxiOptimizerParams.GOAL, Goal.MIN_WAIT_TIME.name());
		params.put(RuleBasedTaxiOptimizerParams.NEAREST_REQUESTS_LIMIT, 10 + "");
		params.put(RuleBasedTaxiOptimizerParams.NEAREST_VEHICLES_LIMIT, 10 + "");
		params.put(RuleBasedTaxiOptimizerParams.CELL_SIZE, 1000 + "");
		runBenchmark(variants, params, benchmark);
	}
}
