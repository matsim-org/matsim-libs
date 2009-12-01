/* *********************************************************************** *
 * project: org.matsim.*
 * BasicDemandGenerationTest
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
package org.matsim.api.basic.v01;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;


/**
 * @author dgrether
 *
 */
public class BasicDemandGenerationTest extends MatsimTestCase {

	
	private static final String populationFile = "population.xml";
	private final double homeEndTime = 9*3600.0;
	private final double workEndTime = 19*3600.0;
	private List<Id> ids = new ArrayList<Id>();
	private Scenario sc = null;
	
	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.sc = new ScenarioImpl();
		for (int i = 1; i <= 6; i++){
			ids.add(sc.createId(Integer.toString(i)));
		}
	}
	
	@Override
	protected void tearDown() throws Exception {
		this.sc = null;
		this.ids = null;
		super.tearDown();
	}
	
	public void testDemandGeneration(){
		Config conf = sc.getConfig();
		assertNotNull(conf);
		
		this.createFakeNetwork(sc, (NetworkLayer)sc.getNetwork());
		
		Population pop = sc.getPopulation();
		PopulationFactory builder = pop.getFactory();
		Person person;
		Plan plan;
		Activity activity;
		Leg leg;
		Route route;
		
		for (int i = 0; i < ids.size(); i++){
			//create the person and add it to the population
			person = builder.createPerson(ids.get(i));
			//person should be created
			assertNotNull(person);
			//but not added to population
			assertEquals(i, pop.getPersons().size());
			pop.addPerson(person);
			assertEquals(i+1, pop.getPersons().size());
			//create the plan and add it to the person
			plan = builder.createPlan();
			assertNotNull(plan);
			assertNull(plan.getPerson());
			assertEquals(0, person.getPlans().size());
			person.addPlan(plan);
			assertEquals(plan.getPerson(), person);
			assertEquals(1, person.getPlans().size());
			//create the plan elements
			activity = builder.createActivityFromLinkId("h", ids.get(0));
			assertNotNull(activity);
			assertEquals(0, plan.getPlanElements().size());
			//this should be called addActivity
			plan.addActivity(activity);
			assertEquals(1, plan.getPlanElements().size());
			activity.setEndTime(homeEndTime);
			
			leg = builder.createLeg(TransportMode.car);
			assertNotNull(leg);
			assertEquals(1, plan.getPlanElements().size());
			plan.addLeg(leg);
			assertEquals(2, plan.getPlanElements().size());
//			route = builder.createRoute(ids.get(0), ids.get(2), ids.subList(1, 2));
//			assertNotNull(route);
//			assertNull(leg.getRoute());
			//we cannot add routes to legs as they cann't be written by the writers
//			leg.setRoute(route);
			
			activity = builder.createActivityFromLinkId("w", ids.get(2));
			assertNotNull(activity);
			activity.setEndTime(workEndTime);
			assertEquals(2, plan.getPlanElements().size());
			plan.addActivity(activity);
			assertEquals(3, plan.getPlanElements().size());
			
			leg = builder.createLeg(TransportMode.car);
			assertNotNull(leg);
			assertEquals(3, plan.getPlanElements().size());
			plan.addLeg(leg);
			assertEquals(4, plan.getPlanElements().size());
//			route = builder.createRoute(ids.get(2), ids.get(0), ids.subList(3, 6));
//			assertNotNull(route);
//			assertNull(leg.getRoute());
			//we cannot add routes to legs as they cann't be written by the writers
//			leg.setRoute(route);
			
			activity = builder.createActivityFromLinkId("h", ids.get(0));
			assertNotNull(activity);
			assertEquals(4, plan.getPlanElements().size());
			plan.addActivity(activity);
			assertEquals(5, plan.getPlanElements().size());
			
		}
		
		//write created population
		PopulationWriter writer = new PopulationWriter(pop);
		Config config = Gbl.getConfig();
		writer.write(this.getOutputDirectory() + populationFile);
		File outfile = new File(this.getOutputDirectory() + populationFile);
		assertTrue(outfile.exists());
		
		
		//read population again, now the code gets really ugly, dirty and worth to refactor...
		ScenarioImpl scenario = new ScenarioImpl();
		PopulationImpl population  = scenario.getPopulation();
		NetworkLayer network = (NetworkLayer)scenario.getNetwork();
		//this is really ugly...
		this.createFakeNetwork(scenario, network);

		MatsimPopulationReader reader = new  MatsimPopulationReader(scenario);
		reader.readFile(outfile.getAbsolutePath());
		checkContent(population);
	}
	
	private void createFakeNetwork(Scenario scenario, NetworkLayer network){
		NodeImpl n1 = network.getFactory().createNode(ids.get(0), scenario.createCoord(0.0, 0.0), null);
		network.getNodes().put(n1.getId(), n1);
		NodeImpl n2 = network.getFactory().createNode(ids.get(1), scenario.createCoord(0.0, 0.0), null);
		network.getNodes().put(n2.getId(), n2);
		for (Id id : ids){
			LinkImpl l = network.getFactory().createLink(id, n1, n2, network, 23.0, 23.0, 23.0, 1.0);
			network.getLinks().put(l.getId(), l);
		}
	}
	
	private void checkContent(PopulationImpl population) {
		assertNotNull(population);
		assertEquals(ids.size(), population.getPersons().size());
		Person pers;
		Plan p;
		for (Id id : ids){
			pers = population.getPersons().get(id);
			assertNotNull(pers);
			assertNotNull(pers.getPlans());
			assertEquals(1, pers.getPlans().size());
			p = pers.getPlans().get(0);
			assertNotNull(p);
			for (int i = 0; i < p.getPlanElements().size(); i++){
				PlanElement element = p.getPlanElements().get(i);
				assertNotNull(element);
			}
			assertEquals(this.homeEndTime, ((PlanImpl) p).getFirstActivity().getEndTime(), EPSILON);
			assertEquals(ids.get(0), ((PlanImpl) p).getFirstActivity().getLinkId());
			assertEquals(this.workEndTime, ((Activity)p.getPlanElements().get(2)).getEndTime(), EPSILON);
			assertEquals(ids.get(2), ((Activity)p.getPlanElements().get(2)).getLinkId());
			assertEquals(Time.UNDEFINED_TIME, ((PlanImpl) p).getLastActivity().getEndTime(), EPSILON);
			assertEquals(ids.get(0), ((PlanImpl) p).getLastActivity().getLinkId());
			
			
			assertEquals(TransportMode.car, ((PlanImpl) p).getNextLeg(((PlanImpl) p).getFirstActivity()).getMode());
			assertNull(((PlanImpl) p).getNextLeg(((PlanImpl) p).getFirstActivity()).getRoute());
			assertEquals(TransportMode.car, ((PlanImpl) p).getPreviousLeg(((PlanImpl) p).getLastActivity()).getMode());
			assertNull(((PlanImpl) p).getPreviousLeg(((PlanImpl) p).getLastActivity()).getRoute());
		}
		
		
	}

}
