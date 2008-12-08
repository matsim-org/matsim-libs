/* *********************************************************************** *
 * project: org.matsim.*
 * ChangeLegModeTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.replanning.modules;

import java.util.HashMap;
import java.util.Map;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.config.Config;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;

/**
 * @author mrieser
 */
public class ChangeLegModeTest extends MatsimTestCase {

	public void testDefaultModes() {
		Config config = loadConfig(null);
		config.global().setNumberOfThreads(0);

		final ChangeLegMode module = new ChangeLegMode();
		final BasicLeg.Mode[] modes = new BasicLeg.Mode[] {BasicLeg.Mode.car, BasicLeg.Mode.pt};
		runTest(module, modes);
	}

	public void testWithConfig() {
		Config config = loadConfig(null);
		config.global().setNumberOfThreads(0);
		config.setParam(ChangeLegMode.CONFIG_MODULE, ChangeLegMode.CONFIG_PARAM_MODES, " car,pt ,bike,walk ");

		final ChangeLegMode module = new ChangeLegMode(config);
		final BasicLeg.Mode[] modes = new BasicLeg.Mode[] {BasicLeg.Mode.car, BasicLeg.Mode.pt, BasicLeg.Mode.bike, BasicLeg.Mode.walk};
		runTest(module, modes);
	}

	private void runTest(final ChangeLegMode module, final BasicLeg.Mode[] possibleModes) {
		module.init();

		Plan plan = new Plan(null);
		plan.createAct("home", new CoordImpl(0, 0));
		Leg leg = plan.createLeg(BasicLeg.Mode.car);
		plan.createAct("work", new CoordImpl(0, 0));

		HashMap<BasicLeg.Mode, Integer> counter = new HashMap<BasicLeg.Mode, Integer>();
		for (BasicLeg.Mode mode : possibleModes) {
			counter.put(mode, Integer.valueOf(0));
		}

		for (int i = 0; i < 10; i++) {
			module.handlePlan(plan);
			Integer count = counter.get(leg.getMode());
			assertNotNull("unexpected mode: " + leg.getMode().toString(), count);
			counter.put(leg.getMode(), Integer.valueOf(count.intValue() + 1));
		}

		for (Map.Entry<BasicLeg.Mode, Integer> entry : counter.entrySet()) {
			int count = entry.getValue().intValue();
			assertTrue("mode " + entry.getKey().toString() + " was never chosen.", count > 0);
		}
	}
}
