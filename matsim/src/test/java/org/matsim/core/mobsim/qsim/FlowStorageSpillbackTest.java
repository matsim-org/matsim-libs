/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.core.mobsim.qsim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

/**
 * Tests the flow capacity for two vehicles that are leaving a link
 * when the storage capacity on the next link (downstream) is reached.
 * 
 * @author ikaddoura
 *
 */
@RunWith(Parameterized.class)
public class FlowStorageSpillbackTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	private final boolean isUsingFastCapacityUpdate;
	
	public FlowStorageSpillbackTest(boolean isUsingFastCapacityUpdate) {
		this.isUsingFastCapacityUpdate = isUsingFastCapacityUpdate;
	}
	
	@Parameters(name = "{index}: isUsingfastCapacityUpdate == {0}")
	public static Collection<Object> parameterObjects () {
		Object [] capacityUpdates = new Object [] { false, true };
		return Arrays.asList(capacityUpdates);
	}
	
	private EventsManager events;
	
	private Id<Person> testAgent1 = Id.create("testAgent1", Person.class);
	private Id<Person> testAgent2 = Id.create("testAgent2", Person.class);
	private Id<Person> testAgent3 = Id.create("testAgent3", Person.class);
	private Id<Person> testAgent4 = Id.create("testAgent4", Person.class);
	
	private Id<Link> linkId1 = Id.create("link1", Link.class);
	private Id<Link> linkId2 = Id.create("link2", Link.class);
	private Id<Link> linkId3 = Id.create("link3", Link.class);
	private Id<Link> linkId4 = Id.create("link4", Link.class);
	
	@Test
	public final void testFlowCongestion(){
		
		Scenario sc = loadScenario();
		setPopulation(sc);
		
		sc.getConfig().qsim().setUsingFastCapacityUpdate(this.isUsingFastCapacityUpdate);
		
		final List<LinkLeaveEvent> linkLeaveEvents = new ArrayList<LinkLeaveEvent>();
							
		events.addHandler( new LinkLeaveEventHandler() {
			
			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(LinkLeaveEvent event) {
				linkLeaveEvents.add(event);
			}			
		});
		
		
		final Map<Id<Person>, Id<Vehicle>> vehicleOfPerson = new HashMap<>();
		
		events.addHandler( new PersonEntersVehicleEventHandler() {
			
			@Override
			public void reset(int iteration) {
			}
			
			@Override
			public void handleEvent(PersonEntersVehicleEvent event) {
				vehicleOfPerson.put(event.getPersonId(), event.getVehicleId());
			}
		});
		
		
		QSim sim = createQSim(sc, events);
		sim.run();

		for (LinkLeaveEvent event : linkLeaveEvents) {
			System.out.println(event.toString());

			if (event.getVehicleId().equals(vehicleOfPerson.get(this.testAgent4)) && event.getLinkId().equals(this.linkId2)) {
				if(this.isUsingFastCapacityUpdate) {
					Assert.assertEquals("wrong link leave time.", 169., event.getTime(), MatsimTestCase.EPSILON);
				} else {
					Assert.assertEquals("wrong link leave time.", 170., event.getTime(), MatsimTestCase.EPSILON);
				}
			}
		}
		
	}
	
	private QSim createQSim(Scenario scenario, EventsManager events) {
		QSim qSim = new QSim(scenario, events);
		ActivityEngine activityEngine = new ActivityEngine(events, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
        QNetsimEngine netsimEngine = new QNetsimEngine(qSim);
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine(scenario, events);
		qSim.addMobsimEngine(teleportationEngine);
		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}
		

	private void setPopulation(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity lastActLink3 = popFactory.createActivityFromLinkId("work", linkId3);
		Activity lastActLink4 = popFactory.createActivityFromLinkId("work", linkId4);
		
		// leg: 3,4
		Leg leg_3_4 = popFactory.createLeg("car");
		List<Id<Link>> linkIds1 = new ArrayList<Id<Link>>();
		NetworkRoute route1 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId4);
		route1.setLinkIds(linkId3, linkIds1, linkId4);
		leg_3_4.setRoute(route1);
		
		// leg: 2,3,4
		Leg leg_2_4 = popFactory.createLeg("car");
		List<Id<Link>> linkIds2 = new ArrayList<Id<Link>>();
		linkIds2.add(linkId3);
		NetworkRoute route2 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId4);
		route2.setLinkIds(linkId2, linkIds2, linkId4);
		leg_2_4.setRoute(route2);
		
		// leg: 1,2,3
		Leg leg_1_3 = popFactory.createLeg("car");
		List<Id<Link>> linkIds3 = new ArrayList<Id<Link>>();
		linkIds3.add(linkId2);
		NetworkRoute route3 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route3.setLinkIds(linkId1, linkIds3, linkId3);
		leg_1_3.setRoute(route3);
		
		// ################################################################
		// first agent activating the flow capacity on link3 (3 --> 4)
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromLinkId("home", linkId3);
		act1.setEndTime(99);
		plan1.addActivity(act1);
		plan1.addLeg(leg_3_4);
		plan1.addActivity(lastActLink4);
		person1.addPlan(plan1);
		population.addPerson(person1);
		
		// ################################################################
		// second agent blocking link3 for 1 min (2 --> 4)
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromLinkId("home", linkId2);
		act2.setEndTime(99);
		plan2.addActivity(act2);
		plan2.addLeg(leg_2_4);
		plan2.addActivity(lastActLink4);
		person2.addPlan(plan2);
		population.addPerson(person2);			
		
		// ################################################################
		// third agent: in buffer of link2 (1 --> 3)
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromLinkId("home", linkId1);
//		act3.setEndTime(104);
		act3.setEndTime(99);
		plan3.addActivity(act3);
		plan3.addLeg(leg_1_3);
		plan3.addActivity(lastActLink3);
		person3.addPlan(plan3);
		population.addPerson(person3);

		// ################################################################
		// last agent causing the troubles... (1 --> 3)		
		Person person4 = popFactory.createPerson(testAgent4);
		Plan plan4 = popFactory.createPlan();
		Activity act4 = popFactory.createActivityFromLinkId("home", linkId1);
//		act4.setEndTime(105);
		act4.setEndTime(100);
		plan4.addActivity(act4);
		plan4.addLeg(leg_1_3);
		plan4.addActivity(lastActLink3);	
		person4.addPlan(plan4);
		population.addPerson(person4);

	}

	private Scenario loadScenario() {
			
		// (0)-----link1-----(1)-----link2-----(2)-----link3-----(3)-----link4-----(4)
		
		Config config = testUtils.loadConfig(null);
		Scenario scenario = (ScenarioUtils.createScenario(config));
	
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setEffectiveCellSize(7.5);
		network.setCapacityPeriod(3600.);

		Node node0 = network.getFactory().createNode(Id.create("0", Node.class), new Coord(0., 0.));
		Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord(100., 0.));
		Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord(200., 0.));
		Node node3 = network.getFactory().createNode(Id.create("3", Node.class), new Coord(300., 0.));
		Node node4 = network.getFactory().createNode(Id.create("4", Node.class), new Coord(400., 0.));
		
		Link link1 = network.getFactory().createLink(this.linkId1, node0, node1);
		Link link2 = network.getFactory().createLink(this.linkId2, node1, node2);
		Link link3 = network.getFactory().createLink(this.linkId3, node2, node3);
		Link link4 = network.getFactory().createLink(this.linkId4, node3, node4);

		Set<String> modes = new HashSet<String>();
		modes.add("car");
		
		// link without capacity restrictions
		link1.setAllowedModes(modes);
		link1.setCapacity(10800);
		link1.setFreespeed(500);
		link1.setNumberOfLanes(100);
		link1.setLength(500);
		
		// link with low flow capacity: one car every 10 sec
		link2.setAllowedModes(modes);
		link2.setCapacity(360);
		link2.setFreespeed(10);
		link2.setNumberOfLanes(100);
		link2.setLength(500);
		
		// link meant to reach storage capacity: space for one car, one car every 60 sec
		link3.setAllowedModes(modes);
		link3.setCapacity(60);
		link3.setFreespeed(500);
		link3.setNumberOfLanes(1);
		link3.setLength(7.5);
		
		// link without capacity restrictions
		link4.setAllowedModes(modes);
		link4.setCapacity(10800);
		link4.setFreespeed(500);
		link4.setNumberOfLanes(100);
		link4.setLength(500);
		
		network.addNode(node0);
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);

		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
		network.addLink(link4);

		this.events = EventsUtils.createEventsManager();
		return scenario;
	}
	
}
