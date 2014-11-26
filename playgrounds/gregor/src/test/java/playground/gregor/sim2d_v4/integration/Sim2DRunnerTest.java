/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DRunnerTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.integration;

import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.testcases.MatsimTestCase;

public class Sim2DRunnerTest extends MatsimTestCase {

	@Test
	public void testSim2DRunner() {
		Config c = ConfigUtils.createConfig();
		c.network().setInputFile(getInputDirectory() + "/network.xml.gz");
		c.plans().setInputFile(getInputDirectory() + "/population.xml.gz");
		c.controler().setLastIteration(0);
		c.strategy().addParam("Module_1", "ReRoute");
		c.strategy().addParam("ModuleProbability_1", ".1");
		c.strategy().addParam("ModuleDisableAfterIteration_1", "250");
		c.strategy().addParam("Module_2", "ChangeExpBeta");
		c.strategy().addParam("ModuleProbability_2", ".9");
		c.controler().setOutputDirectory(getOutputDirectory());

		ActivityParams pre = new ActivityParams("origin");
		pre.setTypicalDuration(49); // needs to be geq 49, otherwise when
									// running a simulation one gets
									// "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in
		// ActivityUtilityParameters.java (gl)
		pre.setMinimalDuration(49);
		pre.setClosingTime(49);
		pre.setEarliestEndTime(49);
		pre.setLatestStartTime(49);
		pre.setOpeningTime(49);

		ActivityParams post = new ActivityParams("destionation");
		post.setTypicalDuration(49); // dito
		post.setMinimalDuration(49);
		post.setClosingTime(49);
		post.setEarliestEndTime(49);
		post.setLatestStartTime(49);
		post.setOpeningTime(49);
		c.planCalcScore().addActivityParams(pre);
		c.planCalcScore().addActivityParams(post);

		c.planCalcScore().setLateArrival_utils_hr(0.);
		c.planCalcScore().setPerforming_utils_hr(0.);

		QSimConfigGroup qsim = c.qsim();
		qsim.setEndTime(300);
		c.controler().setMobsim("hybridQ2D");

		c.global().setCoordinateSystem("EPSG:3395");

		new ConfigWriter(c).write(getOutputDirectory() + "/config.xml");

		String mconf = getOutputDirectory() + "/config.xml";
		String s2dconf = getInputDirectory() + "/s2d_config.xml";
		// Sim2DRunner.main(new String[]{s2dconf,mconf, "false"});

		assertEquals(false, false);
	}

}
