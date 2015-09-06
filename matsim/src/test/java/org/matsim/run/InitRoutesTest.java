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

import java.io.File;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.*;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;

/**
 * Simple test case to ensure that {@link org.matsim.run.InitRoutes} functions properly, e.g. really
 * writes out modified plans. It does <em>not</em> test that the routing algorithms actually produce
 * correct shortest paths or similar.
 *
 * @author mrieser
 */
public class InitRoutesTest extends MatsimTestCase {

	public void testMain() throws Exception {
		Config config = loadConfig(null);
		final String NETWORK_FILE = "test/scenarios/equil/network.xml";
		final String PLANS_FILE_TESTINPUT = getOutputDirectory() + "plans.in.xml";
		final String PLANS_FILE_TESTOUTPUT = getOutputDirectory() + "plans.out.xml";
		final String CONFIG_FILE = getOutputDirectory() + "config.xml";

		// prepare data like world and network
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).parse(NETWORK_FILE);

		// create one person with missing link in act
		Population population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		Person person = PersonImpl.createPerson(Id.create("1", Person.class));
		population.addPerson(person);
		PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
		ActivityImpl a1 = plan.createAndAddActivity("h", Id.create("1", Link.class));
		a1.setEndTime(3600);
		plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("w", Id.create("20", Link.class));

		// write person to file
		new PopulationWriter(population, network).write(PLANS_FILE_TESTINPUT);

		// prepare config for test
		config.network().setInputFile(NETWORK_FILE);
		config.plans().setInputFile(PLANS_FILE_TESTINPUT);
		new ConfigWriter(config).write(CONFIG_FILE);

		// some pre-tests
		assertFalse("Output-File should not yet exist.", new File(PLANS_FILE_TESTOUTPUT).exists());

		// now run the tested class
		InitRoutes.main(new String[] {CONFIG_FILE, PLANS_FILE_TESTOUTPUT});

		// now perform some tests
		assertTrue("no output generated.", new File(PLANS_FILE_TESTOUTPUT).exists());
		Population population2 = scenario.getPopulation();
		new MatsimPopulationReader(scenario).parse(PLANS_FILE_TESTOUTPUT);
		assertEquals("wrong number of persons.", 1, population2.getPersons().size());
		Person person2 = population2.getPersons().get(Id.create("1", Person.class));
		assertNotNull("person 1 missing", person2);
		assertEquals("wrong number of plans in person 1", 1, person2.getPlans().size());
		Plan plan2 = person2.getPlans().get(0);
		LegImpl leg2 = (LegImpl) plan2.getPlanElements().get(1);
		NetworkRoute route2 = (NetworkRoute) leg2.getRoute();
		assertNotNull("no route assigned.", route2);
		assertEquals("wrong route", 2, route2.getLinkIds().size());
	}

}