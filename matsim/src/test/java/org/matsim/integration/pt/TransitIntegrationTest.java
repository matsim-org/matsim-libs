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

package org.matsim.integration.pt;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.testcases.MatsimTestUtils;

public class TransitIntegrationTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void test_RunTutorial() {
		Config config = this.utils.loadConfig("test/scenarios/pt-tutorial/config.xml");
		config.planCalcScore().setWriteExperiencedPlans(true);
		config.controler().setLastIteration(0);
		config.plans().setInputFile("test/scenarios/pt-tutorial/population2.xml");
		Controler controler = new Controler(config);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.getConfig().controler().setCreateGraphs(false);
        controler.run();

		ScenarioImpl s = (ScenarioImpl) controler.getScenario();
		Assert.assertNotNull(s.getTransitSchedule());
		Assert.assertEquals(4, s.getTransitSchedule().getFacilities().size());
		Assert.assertEquals(1, s.getTransitSchedule().getTransitLines().size());
	}
}
