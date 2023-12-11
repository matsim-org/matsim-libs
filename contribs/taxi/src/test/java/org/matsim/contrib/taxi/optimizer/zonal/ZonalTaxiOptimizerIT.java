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

import static org.matsim.contrib.taxi.optimizer.TaxiOptimizerTests.runBenchmark;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedRequestInserter.Goal;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.contrib.zone.ZonalSystemParams;
import org.matsim.testcases.MatsimTestUtils;

public class ZonalTaxiOptimizerIT {
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testZonal_dse() {
		RuleBasedTaxiOptimizerParams rbParams = new RuleBasedTaxiOptimizerParams();
		rbParams.goal = Goal.DEMAND_SUPPLY_EQUIL;
		rbParams.nearestRequestsLimit = 99999;
		rbParams.nearestVehiclesLimit = 99999;
		rbParams.cellSize = 99999.;

		ZonalSystemParams zsParams = new ZonalSystemParams();
		zsParams.zonesShpFile = "zones/zones.shp";
		zsParams.zonesXmlFile = "zones/zones.xml";
		zsParams.expansionDistance = 3000;

		ZonalTaxiOptimizerParams params = new ZonalTaxiOptimizerParams();
		params.addParameterSet(zsParams);
		params.addParameterSet(rbParams);

		runBenchmark(false, params, utils);
	}

	@Test
	void testZonal_minWaitTime() {
		RuleBasedTaxiOptimizerParams rbParams = new RuleBasedTaxiOptimizerParams();
		rbParams.goal = Goal.MIN_WAIT_TIME;
		rbParams.nearestRequestsLimit = 10;
		rbParams.nearestVehiclesLimit = 10;
		rbParams.cellSize = 1000.;

		ZonalSystemParams zsParams = new ZonalSystemParams();
		zsParams.zonesShpFile = "zones/zones.shp";
		zsParams.zonesXmlFile = "zones/zones.xml";
		zsParams.expansionDistance = 3000;

		ZonalTaxiOptimizerParams params = new ZonalTaxiOptimizerParams();
		params.addParameterSet(rbParams);
		params.addParameterSet(zsParams);

		runBenchmark(false, params, utils);
	}
}
