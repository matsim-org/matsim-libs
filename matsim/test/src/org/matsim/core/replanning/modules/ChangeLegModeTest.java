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

package org.matsim.core.replanning.modules;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.replanning.modules.ChangeLegMode;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class ChangeLegModeTest extends MatsimTestCase {

	public void testDefaultModes() {
		Config config = loadConfig(null);
		config.global().setNumberOfThreads(0);

		final ChangeLegMode module = new ChangeLegMode();
		final TransportMode[] modes = new TransportMode[] {TransportMode.car, TransportMode.pt};
		runTest(module, modes);
	}

	public void testWithConfig() {
		Config config = loadConfig(null);
		config.global().setNumberOfThreads(0);
		config.setParam(ChangeLegMode.CONFIG_MODULE, ChangeLegMode.CONFIG_PARAM_MODES, " car,pt ,bike,walk ");

		final ChangeLegMode module = new ChangeLegMode(config);
		final TransportMode[] modes = new TransportMode[] {TransportMode.car, TransportMode.pt, TransportMode.bike, TransportMode.walk};
		runTest(module, modes);
	}

	private void runTest(final ChangeLegMode module, final TransportMode[] possibleModes) {
		module.prepareReplanning();

		Plan plan = new org.matsim.core.population.PlanImpl(null);
		plan.createActivity("home", new CoordImpl(0, 0));
		Leg leg = plan.createLeg(TransportMode.car);
		plan.createActivity("work", new CoordImpl(0, 0));

		HashMap<TransportMode, Integer> counter = new HashMap<TransportMode, Integer>();
		for (TransportMode mode : possibleModes) {
			counter.put(mode, Integer.valueOf(0));
		}

		for (int i = 0; i < 10; i++) {
			module.handlePlan(plan);
			Integer count = counter.get(leg.getMode());
			assertNotNull("unexpected mode: " + leg.getMode().toString(), count);
			counter.put(leg.getMode(), Integer.valueOf(count.intValue() + 1));
		}

		for (Map.Entry<TransportMode, Integer> entry : counter.entrySet()) {
			int count = entry.getValue().intValue();
			assertTrue("mode " + entry.getKey().toString() + " was never chosen.", count > 0);
		}
	}
}
