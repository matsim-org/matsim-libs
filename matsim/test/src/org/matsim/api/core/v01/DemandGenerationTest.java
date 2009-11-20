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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
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
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;


/**
 * @author dgrether
 *
 */
public class DemandGenerationTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(DemandGenerationTest.class);
	
	private static final String populationFile = "population.xml";
	private final double homeEndTime = 9*3600.0;
	private final double workEndTime = 19*3600.0;
	private List<Id> ids = new ArrayList<Id>();
	private Scenario sc = null;
	
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
		
		this.createFakeNetwork(sc, (Network)sc.getNetwork());
		
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
			assertEquals(0, person.getPlans().size());
			person.addPlan(plan);
			assertEquals(person, plan.getPerson());
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
		writer.write(this.getOutputDirectory() + populationFile);
		File outfile = new File(this.getOutputDirectory() + populationFile);
		assertTrue(outfile.exists());
		
		
		//read population again, now the code gets really ugly, dirty and worth to refactor...
		Scenario scenario = new ScenarioImpl();
		Population population  = scenario.getPopulation();
		Network network =scenario.getNetwork();
		//this is really ugly...
		this.createFakeNetwork(scenario, network);

		MatsimPopulationReader reader = new  MatsimPopulationReader(population, (NetworkLayer) network);
		reader.readFile(outfile.getAbsolutePath());
		checkContent(population);
	}
	
	private void createFakeNetwork(Scenario scenario, Network network){
		Coord coord = scenario.createCoord(0,0 ) ;

		Node n1 = network.getFactory().createNode(ids.get(0),coord);
//		((NodeImpl)n1).setCoord(scenario.createCoord(0, 0));		
//		((Map)network.getNodes()).put(n1.getId(), n1);
		network.addNode( n1 ) ;

		Node n2 = network.getFactory().createNode(ids.get(1),coord);
//		((NodeImpl)n2).setCoord(scenario.createCoord(0, 0));
//		((Map)network.getNodes()).put(n2.getId(), n2);
		network.addNode( n2 ) ;

		for (Id id : ids){
//			Link l = ((NetworkLayer)network).getFactory().createLink(id, n1.getId(), n2.getId());
			Link l = network.getFactory().createLink(id, n1.getId(), n2.getId() ) ;
			
//			((Map)network.getLinks()).put(l.getId(), l);
			network.addLink( l ) ;
		}
	}
	
	private void checkContent(Population population) {
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
			assertEquals(this.homeEndTime, ((Activity)p.getPlanElements().get(0)).getEndTime(), EPSILON);
			assertEquals(ids.get(0), ((Activity)p.getPlanElements().get(0)).getLinkId());
			assertEquals(this.workEndTime, ((Activity)p.getPlanElements().get(2)).getEndTime(), EPSILON);
			assertEquals(ids.get(2), ((Activity)p.getPlanElements().get(2)).getLinkId());
			assertEquals(Time.UNDEFINED_TIME, ((Activity)p.getPlanElements().get(4)).getEndTime(), EPSILON);
			assertEquals(ids.get(0), ((Activity)p.getPlanElements().get(4)).getLinkId());
			
			
			assertEquals(TransportMode.car, ((Leg)p.getPlanElements().get(1)).getMode());
			assertNull(((Leg)p.getPlanElements().get(1)).getRoute());
			assertEquals(TransportMode.car, ((Leg)p.getPlanElements().get(3)).getMode());
			assertNull(((Leg)p.getPlanElements().get(3)).getRoute());
		}
		
		
	}

}
