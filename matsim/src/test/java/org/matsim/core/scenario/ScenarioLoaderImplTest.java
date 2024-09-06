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


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils.ScenarioBuilder;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.households.Household;
import org.matsim.households.HouseholdUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser / Senozon AG
 */
public class ScenarioLoaderImplTest {

	@RegisterExtension private MatsimTestUtils util = new MatsimTestUtils();

	@Test
	void testLoadScenario_loadTransitData() {
		// test the create/load sequence:
		{
			ScenarioBuilder builder = new ScenarioBuilder(ConfigUtils.loadConfig(IOUtils.extendUrl(this.util.classInputResourcePath(), "transitConfig.xml")));
			// facilities is there by default????
			Scenario scenario = builder.build() ;
			Assertions.assertEquals(0, scenario.getTransitSchedule().getTransitLines().size());
			Assertions.assertEquals(0, scenario.getTransitSchedule().getFacilities().size());
			ScenarioUtils.loadScenario(scenario);
			Assertions.assertEquals(1, scenario.getTransitSchedule().getTransitLines().size());
			Assertions.assertEquals(2, scenario.getTransitSchedule().getFacilities().size());
		}

		// load directly:
		{
			Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(IOUtils.extendUrl(this.util.classInputResourcePath(), "transitConfig.xml")));
			Assertions.assertEquals(1, scenario.getTransitSchedule().getTransitLines().size());
			Assertions.assertEquals(2, scenario.getTransitSchedule().getFacilities().size());
		}
	}

	@Test
	void testLoadScenario_loadPersonAttributes_nowDeprecated() {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(this.util.classInputResourcePath(), "personAttributesConfig.xml"));
		config.plans().addParam("inputPersonAttributesFile", "personAttributes.xml");
		config.plans().setInsistingOnUsingDeprecatedPersonAttributeFile( true );
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population population = scenario.getPopulation();
		Person person = population.getPersons().get( Id.createPersonId( "1" ) ) ;
		Gbl.assertNotNull( person );
		Assertions.assertEquals("world", person.getAttributes().getAttribute( "hello" ) );
	}

	@Test
	void testLoadScenario_loadPersonAttributes() {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(this.util.classInputResourcePath(), "personAttributesConfig.xml"));
		config.plans().addParam("inputPersonAttributesFile", "personAttributes.xml");
		boolean caughtException=false ;
		Scenario scenario = null ;
		try{
			scenario = ScenarioUtils.loadScenario( config );
		} catch ( Exception ee ) {
			// expected exception
			caughtException = true ;
		}
		Assertions.assertTrue( caughtException );
	}


	@Test
	void testLoadScenario_loadFacilitiesAttributes() {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(this.util.classInputResourcePath(), "facilityAttributesConfig.xml"));
		config.facilities().setInsistingOnUsingDeprecatedFacilitiesAttributeFile(true);
		config.facilities().addParam("inputFacilityAttributesFile", "facilityAttributes.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Assertions.assertEquals(
				"world",
				FacilitiesUtils.getFacilityAttribute(
						scenario.getActivityFacilities().getFacilities().get(Id.create(1, ActivityFacility.class)),
						"hello"),
				"unexpected attribute value");
	}

	@Test
	void testLoadScenario_loadHouseholdAttributes() {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(this.util.classInputResourcePath(), "householdAttributesConfig.xml"));
		config.households().addParam("inputHouseholdAttributesFile", "householdAttributes.xml");
		config.households().setInsistingOnUsingDeprecatedHouseholdsAttributeFile(true);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Assertions.assertEquals(
				"world",
				HouseholdUtils.getHouseholdAttribute(
						scenario.getHouseholds().getHouseholds().get(Id.create(1, Household.class)),
						"hello"),
				"unexpected attribute value");

	}
}
