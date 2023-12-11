/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.replanning;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.minibus.PConstants;
import org.matsim.contrib.minibus.hook.Operator;
import org.matsim.contrib.minibus.hook.PPlan;
import org.matsim.contrib.minibus.routeProvider.PScenarioHelper;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.util.ArrayList;


public class EndRouteExtensionTest {
	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	final void testRun() {

		Operator coop = PScenarioHelper.createCoop2111to2333();

		new File(utils.getOutputDirectory() + PConstants.statsOutputFolder).mkdir();

		ArrayList<String> parameter = new ArrayList<>();
		parameter.add("1000.0");
		parameter.add("0.0");

		EndRouteExtension strat = new EndRouteExtension(parameter);

		PPlan testPlan = null;

		Assertions.assertEquals(1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON, "Compare number of vehicles");
		Assertions.assertEquals("p_2111", coop.getBestPlan().getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_2333", coop.getBestPlan().getStopsToBeServed().get(1).getId().toString(), "Compare end stop");
		Assertions.assertNull(testPlan, "Test plan should be null");

		// buffer too small
		testPlan = strat.run(coop);

		Assertions.assertNull(testPlan, "Test plan should be null");

		parameter = new ArrayList<>();
		parameter.add("1000.0");
		parameter.add("0.5");

		strat = new EndRouteExtension(parameter);

		testPlan = strat.run(coop);

		Assertions.assertNotNull(testPlan, "Test plan should not be null");

		Assertions.assertEquals("p_2111", testPlan.getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_2333", testPlan.getStopsToBeServed().get(1).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3343", testPlan.getStopsToBeServed().get(2).getId().toString(), "Compare end stop");


		parameter = new ArrayList<>();
		parameter.add("2000.0");
		parameter.add("0.5");

		strat = new EndRouteExtension(parameter);

		testPlan = strat.run(coop);

		Assertions.assertNotNull(testPlan, "Test plan should not be null");
		Assertions.assertEquals("p_2111", testPlan.getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_2333", testPlan.getStopsToBeServed().get(1).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3334", testPlan.getStopsToBeServed().get(2).getId().toString(), "Compare end stop");

		coop.getBestPlan().setStopsToBeServed(testPlan.getStopsToBeServed());

		testPlan = strat.run(coop);

		// remaining stops are covered now by the buffer of the otherwise wiggly route
		Assertions.assertNull(testPlan, "Test plan should be null");

	}

	@Test
	final void testRunVShapedRoute() {

		Operator coop = PScenarioHelper.createCoopRouteVShaped();

		new File(utils.getOutputDirectory() + PConstants.statsOutputFolder).mkdir();
		ArrayList<String> parameter = new ArrayList<>();
		parameter.add("1000.0");
		parameter.add("0.0");

		EndRouteExtension strat = new EndRouteExtension(parameter);

		PPlan testPlan = null;

		Assertions.assertEquals(1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON, "Compare number of vehicles");
		Assertions.assertEquals("p_2111", coop.getBestPlan().getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3141", coop.getBestPlan().getStopsToBeServed().get(1).getId().toString(), "Compare middle stop");
		Assertions.assertEquals("p_3222", coop.getBestPlan().getStopsToBeServed().get(2).getId().toString(), "Compare end stop");
		Assertions.assertEquals("p_3141", coop.getBestPlan().getStopsToBeServed().get(3).getId().toString(), "Compare middle stop");
		Assertions.assertNull(testPlan, "Test plan should be null");

		// buffer too small
		testPlan = strat.run(coop);

		Assertions.assertNull(testPlan, "Test plan should be null");

		parameter = new ArrayList<>();
		parameter.add("1000.0");
		parameter.add("0.8");
		parameter.add("true");
		parameter.add("false");

		strat = new EndRouteExtension(parameter);

		testPlan = strat.run(coop);

		Assertions.assertNotNull(testPlan, "Test plan should not be null");

		Assertions.assertEquals("p_2111", testPlan.getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3141", testPlan.getStopsToBeServed().get(1).getId().toString(), "Compare most distant stop in between");
		Assertions.assertEquals("p_3222", testPlan.getStopsToBeServed().get(2).getId().toString(), "Compare former end stop");
		Assertions.assertEquals("p_2223", testPlan.getStopsToBeServed().get(3).getId().toString(), "Compare new end stop");
		Assertions.assertEquals("p_3141", testPlan.getStopsToBeServed().get(4).getId().toString(), "Compare most distant stop in between");

		parameter = new ArrayList<>();
		parameter.add("1000.0");
		parameter.add("0.8");
		parameter.add("true");
		parameter.add("false");

		strat = new EndRouteExtension(parameter);

		testPlan = strat.run(coop);

		Assertions.assertNotNull(testPlan, "Test plan should not be null");
		Assertions.assertEquals("p_2111", testPlan.getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3141", testPlan.getStopsToBeServed().get(1).getId().toString(), "Compare most distant stop in between");
		Assertions.assertEquals("p_3222", testPlan.getStopsToBeServed().get(2).getId().toString(), "Compare former end stop");
		Assertions.assertEquals("p_1323", testPlan.getStopsToBeServed().get(3).getId().toString(), "Compare new end stop");
		Assertions.assertEquals("p_3141", testPlan.getStopsToBeServed().get(4).getId().toString(), "Compare most distant stop in between");

		parameter = new ArrayList<>();
		parameter.add("1500.0");
		parameter.add("0.8");
		parameter.add("true");
		parameter.add("false");

		strat = new EndRouteExtension(parameter);

		testPlan = strat.run(coop);

		Assertions.assertNotNull(testPlan, "Test plan should not be null");
		Assertions.assertEquals("p_2111", testPlan.getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3141", testPlan.getStopsToBeServed().get(1).getId().toString(), "Compare most distant stop in between");
		Assertions.assertEquals("p_3222", testPlan.getStopsToBeServed().get(2).getId().toString(), "Compare former end stop");
		Assertions.assertEquals("p_1413", testPlan.getStopsToBeServed().get(3).getId().toString(), "Compare new end stop");
		Assertions.assertEquals("p_3141", testPlan.getStopsToBeServed().get(4).getId().toString(), "Compare most distant stop in between");

		coop.getBestPlan().setStopsToBeServed(testPlan.getStopsToBeServed());
		coop.getBestPlan().setLine(testPlan.getLine());

		testPlan = strat.run(coop);

		// Adds stop 2414
		Assertions.assertEquals("p_2414", testPlan.getStopsToBeServed().get(4).getId().toString(), "Compare new end stop");

		coop.getBestPlan().setStopsToBeServed(testPlan.getStopsToBeServed());
		coop.getBestPlan().setLine(testPlan.getLine());
		testPlan = strat.run(coop);

		// remaining stops are covered now by the buffer of the otherwise wiggly route
		Assertions.assertNull(testPlan, "Test plan should be null");
	}
}
