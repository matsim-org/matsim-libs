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

import static org.matsim.contrib.taxi.optimizer.TaxiOptimizerTests.runBenchmark;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedRequestInserter.Goal;
import org.matsim.testcases.MatsimTestUtils;

public class RuleBasedTaxiOptimizerIT {
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testRuleBased_dse() {
		RuleBasedTaxiOptimizerParams params = new RuleBasedTaxiOptimizerParams();
		params.goal = Goal.DEMAND_SUPPLY_EQUIL;
		params.nearestRequestsLimit = 99999;
		params.nearestVehiclesLimit = 99999;
		params.cellSize = 99999.;
		runBenchmark(false, params, utils);
	}

	@Test
	void testRuleBased_minWaitTime() {
		RuleBasedTaxiOptimizerParams params = new RuleBasedTaxiOptimizerParams();
		params.goal = Goal.MIN_WAIT_TIME;
		params.nearestRequestsLimit = 10;
		params.nearestVehiclesLimit = 10;
		params.cellSize = 1000.;
		runBenchmark(false, params, utils);
	}

	@Test
	void testRuleBased_minPickupTime() {
		RuleBasedTaxiOptimizerParams params = new RuleBasedTaxiOptimizerParams();
		params.goal = Goal.MIN_PICKUP_TIME;
		params.nearestRequestsLimit = 1;
		params.nearestVehiclesLimit = 1;
		params.cellSize = 100.;
		runBenchmark(false, params, utils);
	}
}
