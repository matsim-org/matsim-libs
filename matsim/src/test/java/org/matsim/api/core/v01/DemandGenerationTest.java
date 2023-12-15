/* *********************************************************************** *
 * project: org.matsim.*
 * DemandGenerationTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.api.core.v01;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dgrether
 */
public class DemandGenerationTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	private static final String populationFile = "population.xml";
	private static final double homeEndTime = 9*3600.0;
	private static final double workEndTime = 19*3600.0;
	private Scenario sc = null;
	private int personCount = 6;
	private int linkCount = 6;

	@BeforeEach public void setUp() {
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}

	@AfterEach public void tearDown() {
		this.sc = null;
	}

	@Test
	void testDemandGeneration(){
		Config conf = sc.getConfig();
		assertNotNull(conf);

		this.createFakeNetwork(sc, sc.getNetwork());

		Population pop = sc.getPopulation();
		PopulationFactory pf = pop.getFactory();
		Person person;
		Plan plan;
		Activity activity;
		Leg leg;

		for (int i = 1; i <= personCount; i++){
			//create the person and add it to the population
			person = pf.createPerson(Id.create(i, Person.class));
			//person should be created
			assertNotNull(person);
			//but not added to population
			assertEquals(i - 1, pop.getPersons().size());
			pop.addPerson(person);
			assertEquals(i, pop.getPersons().size());
			//create the plan and add it to the person
			plan = pf.createPlan();
			assertNotNull(plan);
			assertNull(plan.getPerson());
			assertEquals(0, person.getPlans().size());
			person.addPlan(plan);
			assertEquals(person, plan.getPerson());
			assertEquals(1, person.getPlans().size());
			//create the plan elements
			activity = pf.createActivityFromLinkId("h", Id.create(1, Link.class));
			assertNotNull(activity);
			assertEquals(0, plan.getPlanElements().size());
			//this should be called addActivity
			plan.addActivity(activity);
			assertEquals(1, plan.getPlanElements().size());
			activity.setEndTime(homeEndTime);

			leg = pf.createLeg(TransportMode.car);
			assertNotNull(leg);
			assertEquals(1, plan.getPlanElements().size());
			plan.addLeg(leg);
			assertEquals(2, plan.getPlanElements().size());

			activity = pf.createActivityFromLinkId("w", Id.create(3, Link.class));
			assertNotNull(activity);
			activity.setEndTime(workEndTime);
			assertEquals(2, plan.getPlanElements().size());
			plan.addActivity(activity);
			assertEquals(3, plan.getPlanElements().size());

			leg = pf.createLeg(TransportMode.car);
			assertNotNull(leg);
			assertEquals(3, plan.getPlanElements().size());
			plan.addLeg(leg);
			assertEquals(4, plan.getPlanElements().size());
//			route = builder.createRoute(ids.get(2), ids.get(0), ids.subList(3, 6));
//			assertNotNull(route);
//			assertNull(leg.getRoute());
			//we cannot add routes to legs as they cann't be written by the writers
//			leg.setRoute(route);

			activity = pf.createActivityFromLinkId("h", Id.create(1, Link.class));
			assertNotNull(activity);
			assertEquals(4, plan.getPlanElements().size());
			plan.addActivity(activity);
			assertEquals(5, plan.getPlanElements().size());

		}

		//write created population
		PopulationWriter writer = new PopulationWriter(pop, sc.getNetwork());
		writer.write(utils.getOutputDirectory() + populationFile);
		File outfile = new File(utils.getOutputDirectory() + populationFile);
		assertTrue(outfile.exists());


		//read population again, now the code gets really ugly, dirty and worth to refactor...
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population  = scenario.getPopulation();
		Network network =scenario.getNetwork();
		//this is really ugly...
		this.createFakeNetwork(scenario, network);

		PopulationReader reader = new  PopulationReader(scenario);
		reader.readFile(outfile.getAbsolutePath());
		checkContent(population);
	}

	private void createFakeNetwork(Scenario scenario, Network network){
		Coord coord = new Coord((double) 0, (double) 0);

		Node n1 = network.getFactory().createNode(Id.create(0, Node.class), coord);
		network.addNode( n1 ) ;

		Node n2 = network.getFactory().createNode(Id.create(1, Node.class), coord);
		network.addNode( n2 ) ;

		for (int i = 1; i <= linkCount; i++) {
			Link l = network.getFactory().createLink(Id.create(i, Link.class), n1, n2);
			network.addLink( l ) ;
		}
	}

	private void checkContent(Population population) {
		assertNotNull(population);
		assertEquals(linkCount, personCount);
		Person pers;
		Plan p;
		for (int id = 1; id <= linkCount; id++) {
			pers = population.getPersons().get(Id.create(id, Person.class));
			assertNotNull(pers);
			assertNotNull(pers.getPlans());
			assertEquals(1, pers.getPlans().size());
			p = pers.getPlans().get(0);
			assertNotNull(p);
			for (int i = 0; i < p.getPlanElements().size(); i++) {
				PlanElement element = p.getPlanElements().get(i);
				assertNotNull(element);
			}
			assertEquals(homeEndTime, ((Activity)p.getPlanElements().get(0)).getEndTime().seconds(), MatsimTestUtils.EPSILON);
			assertEquals(Id.create(1, Link.class), ((Activity)p.getPlanElements().get(0)).getLinkId());
			assertEquals(workEndTime, ((Activity)p.getPlanElements().get(2)).getEndTime().seconds(), MatsimTestUtils.EPSILON);
			assertEquals(Id.create(3, Link.class), ((Activity)p.getPlanElements().get(2)).getLinkId());
			assertTrue(((Activity)p.getPlanElements().get(4)).getEndTime().isUndefined());
			assertEquals(Id.create(1, Link.class), ((Activity)p.getPlanElements().get(4)).getLinkId());

			assertEquals(TransportMode.car, ((Leg)p.getPlanElements().get(1)).getMode());
			assertNull(((Leg)p.getPlanElements().get(1)).getRoute());
			assertEquals(TransportMode.car, ((Leg)p.getPlanElements().get(3)).getMode());
			assertNull(((Leg)p.getPlanElements().get(3)).getRoute());
		}
	}

}
