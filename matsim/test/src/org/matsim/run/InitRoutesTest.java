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

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.NetworkRouteWRefs;
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
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).parse(NETWORK_FILE);

		// create one person with missing link in act
		PopulationImpl population = new PopulationImpl();
		PersonImpl person = new PersonImpl(new IdImpl("1"));
		population.getPersons().put(person.getId(), person);
		PlanImpl plan = person.createAndAddPlan(true);
		ActivityImpl a1 = plan.createAndAddActivity("h", network.getLink(new IdImpl("1")));
		a1.setEndTime(3600);
		plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("w", network.getLink(new IdImpl("20")));

		// write person to file
		new PopulationWriter(population).writeFile(PLANS_FILE_TESTINPUT);

		// prepare config for test
		config.network().setInputFile(NETWORK_FILE);
		config.plans().setInputFile(PLANS_FILE_TESTINPUT);
		config.plans().setOutputFile(PLANS_FILE_TESTOUTPUT);
		new ConfigWriter(config).writeFile(CONFIG_FILE);
		Gbl.reset(); // needed to delete the global config etc for the test

		// some pre-tests
		assertFalse("Output-File should not yet exist.", new File(PLANS_FILE_TESTOUTPUT).exists());

		// now run the tested class
		InitRoutes.main(new String[] {CONFIG_FILE});

		// now perform some tests
		assertTrue("no output generated.", new File(PLANS_FILE_TESTOUTPUT).exists());
		PopulationImpl population2 = new PopulationImpl();
		new MatsimPopulationReader(population2, network).parse(PLANS_FILE_TESTOUTPUT);
		assertEquals("wrong number of persons.", 1, population2.getPersons().size());
		PersonImpl person2 = population2.getPersons().get(new IdImpl("1"));
		assertNotNull("person 1 missing", person2);
		assertEquals("wrong number of plans in person 1", 1, person2.getPlans().size());
		Plan plan2 = person2.getPlans().get(0);
		LegImpl leg2 = (LegImpl) plan2.getPlanElements().get(1);
		NetworkRouteWRefs route2 = (NetworkRouteWRefs) leg2.getRoute();
		assertNotNull("no route assigned.", route2);
		assertEquals("wrong route", 3, route2.getNodes().size());
	}

}