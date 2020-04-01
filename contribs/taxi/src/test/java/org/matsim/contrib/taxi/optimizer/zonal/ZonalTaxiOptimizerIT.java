/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer.zonal;

import static org.matsim.contrib.taxi.optimizer.TaxiOptimizerTests.createDefaultTaxiConfigVariants;
import static org.matsim.contrib.taxi.optimizer.TaxiOptimizerTests.runBenchmark;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerTests.PreloadedBenchmark;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerTests.TaxiConfigVariant;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedRequestInserter.Goal;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.contrib.zone.ZonalSystemParams;
import org.matsim.testcases.MatsimTestUtils;

public class ZonalTaxiOptimizerIT {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testZonal() {
		PreloadedBenchmark benchmark = new PreloadedBenchmark("3.0", "25");
		List<TaxiConfigVariant> variants = createDefaultTaxiConfigVariants(false);
		RuleBasedTaxiOptimizerParams rbParams = new RuleBasedTaxiOptimizerParams();
		ZonalSystemParams zsParams = new ZonalSystemParams();
		zsParams.setZonesShpFile("zones/zones.shp");
		zsParams.setZonesXmlFile("zones/zones.xml");
		ZonalTaxiOptimizerParams params = new ZonalTaxiOptimizerParams();
		params.addParameterSet(rbParams);
		params.addParameterSet(zsParams);

		rbParams.setGoal(Goal.DEMAND_SUPPLY_EQUIL);
		rbParams.setNearestRequestsLimit(99999);
		rbParams.setNearestVehiclesLimit(99999);
		rbParams.setCellSize(99999.);
		runBenchmark(variants, params, benchmark, utils.getOutputDirectory() + "_A");

		rbParams.setGoal(Goal.MIN_WAIT_TIME);
		rbParams.setNearestRequestsLimit(10);
		rbParams.setNearestVehiclesLimit(10);
		rbParams.setCellSize(1000.);
		runBenchmark(variants, params, benchmark, utils.getOutputDirectory() + "_B");
	}
}
