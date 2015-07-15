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
 * @author mrieser / Senozon AG
 */
public class ScenarioLoaderImplTest {

	@Rule public MatsimTestUtils util = new MatsimTestUtils();

	@Test
	public void testLoadScenario_loadTransitData() {
		Scenario scenario = ScenarioUtils.createScenario(this.util.loadConfig(this.util.getClassInputDirectory() + "transitConfig.xml"));
		Assert.assertEquals(0, scenario.getTransitSchedule().getTransitLines().size());
		Assert.assertEquals(0, scenario.getTransitSchedule().getFacilities().size());
		ScenarioUtils.loadScenario(scenario);
		Assert.assertEquals(1, scenario.getTransitSchedule().getTransitLines().size());
		Assert.assertEquals(2, scenario.getTransitSchedule().getFacilities().size());
	}

	@Test
	public void testLoadScenario_loadPersonAttributes() {
		Config config = this.util.loadConfig(this.util.getClassInputDirectory() + "personAttributesConfig.xml");
		config.plans().addParam("inputPersonAttributesFile", this.util.getClassInputDirectory() + "personAttributes.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Assert.assertEquals("world", scenario.getPopulation().getPersonAttributes().getAttribute("1", "hello"));
	}

	@Test
	public void testLoadScenario_loadTransitLinesAttributes() {
		Config config = this.util.loadConfig(this.util.getClassInputDirectory() + "transitConfig.xml");
		config.transit().setTransitLinesAttributesFile(this.util.getClassInputDirectory() + "transitLinesAttributes.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Assert.assertEquals("world", scenario.getTransitSchedule().getTransitLinesAttributes().getAttribute("Blue Line", "hello"));
	}

	@Test
	public void testLoadScenario_loadTransitStopsAttributes() {
		Config config = this.util.loadConfig(this.util.getClassInputDirectory() + "transitConfig.xml");
		config.transit().setTransitStopsAttributesFile(this.util.getClassInputDirectory() + "transitStopsAttributes.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Assert.assertEquals(Boolean.TRUE, scenario.getTransitSchedule().getTransitStopsAttributes().getAttribute("1", "hasP+R"));
	}

	@Test
	public void testLoadScenario_loadFacilitiesAttributes() {
		Config config = this.util.loadConfig(this.util.getClassInputDirectory() + "facilityAttributesConfig.xml");
		config.facilities().addParam("inputFacilityAttributesFile", this.util.getClassInputDirectory() + "facilityAttributes.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Assert.assertEquals("world", scenario.getActivityFacilities().getFacilityAttributes().getAttribute("1", "hello"));
	}

	@Test
	public void testLoadScenario_loadHouseholdAttributes() {
		Config config = this.util.loadConfig(this.util.getClassInputDirectory() + "householdAttributesConfig.xml");
		config.scenario().setUseHouseholds(true);
		config.households().addParam("inputHouseholdAttributesFile", this.util.getClassInputDirectory() + "householdAttributes.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Assert.assertEquals("world", scenario.getHouseholds().getHouseholdAttributes().getAttribute("1", "hello"));
	}
}
