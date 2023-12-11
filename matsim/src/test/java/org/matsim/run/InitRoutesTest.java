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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
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
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Simple test case to ensure that {@link org.matsim.run.InitRoutes} functions properly, e.g. really
 * writes out modified plans. It does <em>not</em> test that the routing algorithms actually produce
 * correct shortest paths or similar.
 *
 * @author mrieser
 */
public class InitRoutesTest {

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
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORK_FILE);

		// create one person with missing link in act
		Population population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		Person person = PopulationUtils.getFactory().createPerson(Id.create("1", Person.class));
		population.addPerson(person);
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity a1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", Id.create("1", Link.class));
		a1.setEndTime(3600);
		PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", Id.create("20", Link.class));

		// write person to file
		new PopulationWriter(population, network).write(PLANS_FILE_TESTINPUT);

		// prepare config for test
		config.network().setInputFile(NETWORK_FILE);
		config.plans().setInputFile(PLANS_FILE_TESTINPUT);
		new ConfigWriter(config).write(CONFIG_FILE);

		// some pre-tests
		assertFalse(new File(PLANS_FILE_TESTOUTPUT).exists(), "Output-File should not yet exist.");

		// now run the tested class
		InitRoutes.main(new String[] {CONFIG_FILE, PLANS_FILE_TESTOUTPUT});

		// now perform some tests
		assertTrue(new File(PLANS_FILE_TESTOUTPUT).exists(), "no output generated.");
		Population population2 = scenario.getPopulation();
		new PopulationReader(scenario).readFile(PLANS_FILE_TESTOUTPUT);
		assertEquals(1, population2.getPersons().size(), "wrong number of persons.");
		Person person2 = population2.getPersons().get(Id.create("1", Person.class));
		assertNotNull(person2, "person 1 missing");
		assertEquals(1, person2.getPlans().size(), "wrong number of plans in person 1");
		Plan plan2 = person2.getPlans().get(0);
		Leg leg2 = (Leg) plan2.getPlanElements().get(1);
		NetworkRoute route2 = (NetworkRoute) leg2.getRoute();
		assertNotNull(route2, "no route assigned.");
		assertEquals(2, route2.getLinkIds().size(), "wrong route");
	}

}
