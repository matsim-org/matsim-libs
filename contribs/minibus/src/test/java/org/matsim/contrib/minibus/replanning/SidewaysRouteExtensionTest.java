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


/*
 * This test might fail if it is run as part of all Minibus-Tests in IntelliJ or Eclipse,
 * but it runs correctly when being run from Maven or individually.
 * This is likely due relying somewhere on an internal iteration order (likely in IdMap), which
 * may be different if other tests have run before in the same JVM and thus Id-indices are different
 * than when running this test alone.
 *
 * For Maven, the surefire-plugin can be configured to run each test individually in a separate JVM which
 * solves this problem, but I don't know how to solve this in IntelliJ or Eclipse.
 * -mrieser/2019Sept30
 */
public class SidewaysRouteExtensionTest {
	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	final void testRun() {

		Operator coop = PScenarioHelper.createCoop2414to3444();

		new File(utils.getOutputDirectory() + PConstants.statsOutputFolder).mkdir();

		ArrayList<String> parameter = new ArrayList<>();
		parameter.add("0.0");
		parameter.add("0.0");
		parameter.add("true");

		SidewaysRouteExtension strat = new SidewaysRouteExtension(parameter);

		PPlan testPlan = null;

		Assertions.assertEquals(1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON, "Compare number of vehicles");
		Assertions.assertEquals("p_2414", coop.getBestPlan().getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3444", coop.getBestPlan().getStopsToBeServed().get(1).getId().toString(), "Compare end stop");
		Assertions.assertNull(testPlan, "Test plan should be null");

		// buffer too small
		testPlan = strat.run(coop);

		Assertions.assertNull(testPlan, "Test plan should be null");

		parameter = new ArrayList<>();
		parameter.add("100.0");
		parameter.add("0.0");
		parameter.add("true");

		strat = new SidewaysRouteExtension(parameter);

		testPlan = strat.run(coop);

		// enough buffer to add a stop located directly at the beeline
		Assertions.assertNotNull(testPlan, "Test plan should not be null");

		Assertions.assertEquals("p_2414", testPlan.getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_2324", testPlan.getStopsToBeServed().get(1).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3444", testPlan.getStopsToBeServed().get(2).getId().toString(), "Compare end stop");
		Assertions.assertEquals("p_2324", testPlan.getStopsToBeServed().get(3).getId().toString(), "Compare end stop");


		parameter = new ArrayList<>();
		parameter.add("100.0");
		parameter.add("0.5");
		parameter.add("true");

		strat = new SidewaysRouteExtension(parameter);

		testPlan = strat.run(coop);

		// enough buffer 0.5 * 3000m = 1500m
		Assertions.assertNotNull(testPlan, "Test plan should not be null");
		Assertions.assertEquals("p_2414", testPlan.getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_2223", testPlan.getStopsToBeServed().get(1).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3444", testPlan.getStopsToBeServed().get(2).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_2223", testPlan.getStopsToBeServed().get(3).getId().toString(), "Compare end stop");

		coop.getBestPlan().setStopsToBeServed(testPlan.getStopsToBeServed());
		coop.getBestPlan().setLine(coop.getRouteProvider().createTransitLineFromOperatorPlan(coop.getId(), testPlan));

		testPlan = strat.run(coop);

		// and again stacking - therefore, enlarging the effective buffer
		Assertions.assertNotNull(testPlan, "Test plan should not be null");
		Assertions.assertEquals("p_2414", testPlan.getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_2212", testPlan.getStopsToBeServed().get(1).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_2223", testPlan.getStopsToBeServed().get(2).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3444", testPlan.getStopsToBeServed().get(3).getId().toString(), "Compare end stop");
		Assertions.assertEquals("p_2212", testPlan.getStopsToBeServed().get(4).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_2223", testPlan.getStopsToBeServed().get(5).getId().toString(), "Compare end stop");

		parameter = new ArrayList<>();
		parameter.add("4000.0");
		parameter.add("0.5");
		parameter.add("true");

		strat = new SidewaysRouteExtension(parameter);
		coop = PScenarioHelper.createCoop2414to3444();

		testPlan = strat.run(coop);

		// quite a lot buffer covering all nodes
		Assertions.assertNotNull(testPlan, "Test plan should not be null");
		Assertions.assertEquals("p_2414", testPlan.getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_2324", testPlan.getStopsToBeServed().get(1).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3444", testPlan.getStopsToBeServed().get(2).getId().toString(), "Compare end stop");
		Assertions.assertEquals("p_2324", testPlan.getStopsToBeServed().get(3).getId().toString(), "Compare end stop");

		testPlan = strat.run(coop);

		// quite a lot buffer covering all nodes
		Assertions.assertNotNull(testPlan, "Test plan should not be null");
		Assertions.assertEquals("p_2414", testPlan.getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_2223", testPlan.getStopsToBeServed().get(1).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3444", testPlan.getStopsToBeServed().get(2).getId().toString(), "Compare end stop");
		Assertions.assertEquals("p_2223", testPlan.getStopsToBeServed().get(3).getId().toString(), "Compare end stop");

		testPlan = strat.run(coop);

		// quite a lot buffer covering all nodes
		Assertions.assertNotNull(testPlan, "Test plan should not be null");
		Assertions.assertEquals("p_2414", testPlan.getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_2223", testPlan.getStopsToBeServed().get(1).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3444", testPlan.getStopsToBeServed().get(2).getId().toString(), "Compare end stop");
		Assertions.assertEquals("p_2223", testPlan.getStopsToBeServed().get(3).getId().toString(), "Compare end stop");

		testPlan = strat.run(coop);

		// quite a lot buffer covering all nodes
		Assertions.assertNotNull(testPlan, "Test plan should not be null");
		Assertions.assertEquals("p_2414", testPlan.getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3323", testPlan.getStopsToBeServed().get(1).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3444", testPlan.getStopsToBeServed().get(2).getId().toString(), "Compare end stop");
		Assertions.assertEquals("p_3323", testPlan.getStopsToBeServed().get(3).getId().toString(), "Compare end stop");

		testPlan = strat.run(coop);

		// quite a lot buffer covering all nodes
		Assertions.assertNotNull(testPlan, "Test plan should not be null");
		Assertions.assertEquals("p_2414", testPlan.getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3433", testPlan.getStopsToBeServed().get(1).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3444", testPlan.getStopsToBeServed().get(2).getId().toString(), "Compare end stop");
		Assertions.assertEquals("p_3433", testPlan.getStopsToBeServed().get(3).getId().toString(), "Compare end stop");

		testPlan = strat.run(coop);

		// quite a lot buffer covering all nodes
		Assertions.assertNotNull(testPlan, "Test plan should not be null");
		Assertions.assertEquals("p_2414", testPlan.getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_2423", testPlan.getStopsToBeServed().get(1).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3444", testPlan.getStopsToBeServed().get(2).getId().toString(), "Compare end stop");
		Assertions.assertEquals("p_2423", testPlan.getStopsToBeServed().get(3).getId().toString(), "Compare end stop");

		testPlan = strat.run(coop);

		// quite a lot buffer covering all nodes
		Assertions.assertNotNull(testPlan, "Test plan should not be null");
		Assertions.assertEquals("p_2414", testPlan.getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_2322", testPlan.getStopsToBeServed().get(1).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3444", testPlan.getStopsToBeServed().get(2).getId().toString(), "Compare end stop");
		Assertions.assertEquals("p_2322", testPlan.getStopsToBeServed().get(3).getId().toString(), "Compare end stop");

		testPlan = strat.run(coop);

		// quite a lot buffer covering all nodes
		Assertions.assertNotNull(testPlan, "Test plan should not be null");
		Assertions.assertEquals("p_2414", testPlan.getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_2221", testPlan.getStopsToBeServed().get(1).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3444", testPlan.getStopsToBeServed().get(2).getId().toString(), "Compare end stop");
		Assertions.assertEquals("p_2221", testPlan.getStopsToBeServed().get(3).getId().toString(), "Compare end stop");

		parameter = new ArrayList<>();
		parameter.add("100.0");
		parameter.add("0.0");
		parameter.add("false");

		strat = new SidewaysRouteExtension(parameter);
		coop = PScenarioHelper.createCoop2414to3444();

		testPlan = strat.run(coop);

		// can now choose among stops at the outer edges
		Assertions.assertNotNull(testPlan, "Test plan should not be null");
		Assertions.assertEquals("p_2414", testPlan.getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_2324", testPlan.getStopsToBeServed().get(1).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3444", testPlan.getStopsToBeServed().get(2).getId().toString(), "Compare end stop");
		Assertions.assertEquals("p_2324", testPlan.getStopsToBeServed().get(3).getId().toString(), "Compare end stop");

		testPlan = strat.run(coop);

		// can now choose among stops at the outer edges
		Assertions.assertNotNull(testPlan, "Test plan should not be null");
		Assertions.assertEquals("p_2414", testPlan.getStopsToBeServed().get(0).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_B", testPlan.getStopsToBeServed().get(1).getId().toString(), "Compare start stop");
		Assertions.assertEquals("p_3444", testPlan.getStopsToBeServed().get(2).getId().toString(), "Compare end stop");
		Assertions.assertEquals("p_B", testPlan.getStopsToBeServed().get(3).getId().toString(), "Compare end stop");

	}
}
