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


import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils.ScenarioBuilder;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser / Senozon AG
 */
public class ScenarioLoaderImplTest {

	@Rule public MatsimTestUtils util = new MatsimTestUtils();

	@Test
	public void testLoadScenario_loadTransitData() {
		// test the create/load sequence:
		{
			ScenarioBuilder builder = new ScenarioBuilder(ConfigUtils.loadConfig(IOUtils.newUrl(this.util.classInputResourcePath(), "transitConfig.xml")));
			// facilities is there by default????
			Scenario scenario = builder.build() ;
			Assert.assertEquals(0, scenario.getTransitSchedule().getTransitLines().size());
			Assert.assertEquals(0, scenario.getTransitSchedule().getFacilities().size());
			ScenarioUtils.loadScenario(scenario);
			Assert.assertEquals(1, scenario.getTransitSchedule().getTransitLines().size());
			Assert.assertEquals(2, scenario.getTransitSchedule().getFacilities().size());
		}

		// load directly:
		{
			Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(IOUtils.newUrl(this.util.classInputResourcePath(), "transitConfig.xml")));
			Assert.assertEquals(1, scenario.getTransitSchedule().getTransitLines().size());
			Assert.assertEquals(2, scenario.getTransitSchedule().getFacilities().size());
		}
	}

	@Test
	public void testLoadScenario_loadPersonAttributes() {
		Config config = ConfigUtils.loadConfig(IOUtils.newUrl(this.util.classInputResourcePath(), "personAttributesConfig.xml"));
		config.plans().addParam("inputPersonAttributesFile", "personAttributes.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population population = scenario.getPopulation();
		Person person = population.getPersons().get( Id.createPersonId( "1" ) ) ;
		Gbl.assertNotNull( person );
		Assert.assertEquals("world", PopulationUtils.getPersonAttribute( "hello", person, population.getPersonAttributes() ) );
	}

	@Test
	public void testLoadScenario_loadTransitLinesAttributes() {
		Config config = ConfigUtils.loadConfig(IOUtils.newUrl(this.util.classInputResourcePath(), "transitConfig.xml"));
		config.transit().setTransitLinesAttributesFile("transitLinesAttributes.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Assert.assertEquals("world", scenario.getTransitSchedule().getTransitLinesAttributes().getAttribute("Blue Line", "hello"));
	}

	@Test
	public void testLoadScenario_loadTransitStopsAttributes() {
		Config config = ConfigUtils.loadConfig(IOUtils.newUrl(this.util.classInputResourcePath(), "transitConfig.xml"));
		config.transit().setTransitStopsAttributesFile("transitStopsAttributes.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Assert.assertEquals(Boolean.TRUE, scenario.getTransitSchedule().getTransitStopsAttributes().getAttribute("1", "hasP+R"));
	}

	@Test
	public void testLoadScenario_loadFacilitiesAttributes() {
		Config config = ConfigUtils.loadConfig(IOUtils.newUrl(this.util.classInputResourcePath(), "facilityAttributesConfig.xml"));
		config.facilities().addParam("inputFacilityAttributesFile", "facilityAttributes.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Assert.assertEquals("world", scenario.getActivityFacilities().getFacilityAttributes().getAttribute("1", "hello"));
	}

	@Test
	public void testLoadScenario_loadHouseholdAttributes() {
		Config config = ConfigUtils.loadConfig(IOUtils.newUrl(this.util.classInputResourcePath(), "householdAttributesConfig.xml"));
		config.households().addParam("inputHouseholdAttributesFile", "householdAttributes.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Assert.assertEquals("world", scenario.getHouseholds().getHouseholdAttributes().getAttribute("1", "hello"));
	}
}
