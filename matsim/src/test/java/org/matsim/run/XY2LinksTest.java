/* *********************************************************************** *
 * project: org.matsim.*
 * RoutingTest.java
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

package org.matsim.run;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Simple test case to ensure that {@link org.matsim.run.XY2Links} functions properly, e.g. really
 * writes out modified plans. It does <em>not</em> test that {@link org.matsim.core.population.algorithms.XY2Links}
 * works correctly, e.g. that it assigns the right links.
 *
 * @author mrieser
 */
public class XY2LinksTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testMain() throws Exception {
		Config config = utils.loadConfig((String)null);
		final String NETWORK_FILE = "test/scenarios/equil/network.xml";
		final String PLANS_FILE_TESTINPUT = utils.getOutputDirectory() + "plans.in.xml";
		final String PLANS_FILE_TESTOUTPUT = utils.getOutputDirectory() + "plans.out.xml";
		final String CONFIG_FILE = utils.getOutputDirectory() + "config.xml";

		// prepare data like world and network
		MutableScenario scenario =  (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORK_FILE);

		// create one person with missing link in act
		Population population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		Person person = PopulationUtils.getFactory().createPerson(Id.create("1", Person.class));
		population.addPerson(person);
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity a1 = PopulationUtils.createAndAddActivityFromCoord(plan, "h", new Coord(50, 25));
		a1.setEndTime(3600);

		// write person to file
		new PopulationWriter(population, network).write(PLANS_FILE_TESTINPUT);

		// prepare config for test
		config.network().setInputFile(NETWORK_FILE);
		config.plans().setInputFile(PLANS_FILE_TESTINPUT);
		new ConfigWriter(config).write(CONFIG_FILE);

		// some pre-tests
		assertFalse(new File(PLANS_FILE_TESTOUTPUT).exists(), "Output-File should not yet exist.");

		// now run the tested class
		XY2Links.main(new String[] {CONFIG_FILE, PLANS_FILE_TESTOUTPUT});

		// now perform some tests
		assertTrue(new File(PLANS_FILE_TESTOUTPUT).exists(), "no output generated.");
		Population population2 = scenario.getPopulation();
		new PopulationReader(scenario).readFile(PLANS_FILE_TESTOUTPUT);
		assertEquals(1, population2.getPersons().size(), "wrong number of persons.");
		Person person2 = population2.getPersons().get(Id.create("1", Person.class));
		assertNotNull(person2, "person 1 missing");
		assertEquals(1, person2.getPlans().size(), "wrong number of plans in person 1");
		Plan plan2 = person2.getPlans().get(0);
		Activity act2 = (Activity) plan2.getPlanElements().get(0);
		assertNotNull(act2.getLinkId(), "no link assigned.");
	}

}
