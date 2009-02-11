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

import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.config.ConfigWriter;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.population.routes.CarRoute;
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
		Population population = new Population(Population.NO_STREAMING);
		Person person = new PersonImpl(new IdImpl("1"));
		population.addPerson(person);
		Plan plan = person.createPlan(true);
		Act a1 = plan.createAct("h", network.getLink(new IdImpl("1")));
		a1.setEndTime(3600);
		plan.createLeg(BasicLeg.Mode.car);
		plan.createAct("w", network.getLink(new IdImpl("20")));
		
		// write person to file
		new PopulationWriter(population, PLANS_FILE_TESTINPUT, "v4").write();

		// prepare config for test
		config.network().setInputFile(NETWORK_FILE);
		config.plans().setInputFile(PLANS_FILE_TESTINPUT);
		config.plans().setOutputFile(PLANS_FILE_TESTOUTPUT);
		new ConfigWriter(config, CONFIG_FILE).write();
		Gbl.reset(); // needed to delete the global config etc for the test
		
		// some pre-tests
		assertFalse("Output-File should not yet exist.", new File(PLANS_FILE_TESTOUTPUT).exists());
		
		// now run the tested class
		InitRoutes.main(new String[] {CONFIG_FILE});
		
		// now perform some tests
		assertTrue("no output generated.", new File(PLANS_FILE_TESTOUTPUT).exists());
		Population population2 = new Population(Population.NO_STREAMING);
		new MatsimPopulationReader(population2).parse(PLANS_FILE_TESTOUTPUT);
		assertEquals("wrong number of persons.", 1, population2.getPersons().size());
		Person person2 = population2.getPerson(new IdImpl("1"));
		assertNotNull("person 1 missing", person2);
		assertEquals("wrong number of plans in person 1", 1, person2.getPlans().size());
		Plan plan2 = person2.getPlans().get(0);
		Leg leg2 = (Leg) plan2.getActsLegs().get(1);
		CarRoute route2 = (CarRoute) leg2.getRoute();
		assertNotNull("no route assigned.", route2);
		assertEquals("wrong route", 3, route2.getNodes().size());
	}

}