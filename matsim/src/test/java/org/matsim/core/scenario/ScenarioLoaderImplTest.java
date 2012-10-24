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

package org.matsim.core.scenario;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class ScenarioLoaderImplTest {

	@Rule public MatsimTestUtils util = new MatsimTestUtils();

	@Test
	public void testLoadScenario_loadTransitData() {
		Scenario scenario = ScenarioUtils.createScenario(util.loadConfig(util.getClassInputDirectory() + "transitConfig.xml"));
		Assert.assertEquals(0, scenario.getTransitSchedule().getTransitLines().size());
		Assert.assertEquals(0, scenario.getTransitSchedule().getFacilities().size());
		ScenarioUtils.loadScenario(scenario);
		Assert.assertEquals(1, scenario.getTransitSchedule().getTransitLines().size());
		Assert.assertEquals(2, scenario.getTransitSchedule().getFacilities().size());
	}
	
	@Test
	public void testLoadScenario_loadPersonAttributes() {
		Config config = util.loadConfig(util.getClassInputDirectory() + "personAttributesConfig.xml");
		config.plans().addParam("inputPersonAttributesFile", util.getClassInputDirectory() + "personAttributes.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Assert.assertEquals("world", scenario.getPopulation().getPersonAttributes().getAttribute("1", "hello"));
	}
}
