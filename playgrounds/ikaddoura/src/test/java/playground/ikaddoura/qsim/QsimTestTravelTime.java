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
package playground.ikaddoura.qsim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura, lkroeger
 *
 */

public class QsimTestTravelTime {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	private EventsManager events;
	
	private Id<Person> testAgent1 = Id.create("testAgent1", Person.class);
	
	private Id<Link> linkId1 = Id.create("link1", Link.class);
	private Id<Link> linkId2 = Id.create("link2", Link.class);
	private Id<Link> linkId3 = Id.create("link3", Link.class);
	private Id<Link> linkId4 = Id.create("link4", Link.class);
	private Id<Link> linkId5 = Id.create("link5", Link.class);
	
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	
	// the travel time on single links is tested,
	// the length and the velocity are varied
	@Test
	public final void testTravelTime(){
		
		Scenario sc = loadScenario();
		setPopulation(sc);
		
		final List<LinkEnterEvent> linkEnterEvents = new ArrayList<LinkEnterEvent>();
		
		events.addHandler( new LinkEnterEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(LinkEnterEvent event) {
				System.out.println(event.toString());
				linkEnterEvents.add(event);
			}	
		});
		
		events.addHandler( new LinkLeaveEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(LinkLeaveEvent event) {
				System.out.println(event.toString());
			}	
		});
						
		QSim sim = createQSim(sc, events);
		sim.run();
		
		for (LinkEnterEvent event : linkEnterEvents) {
//			
				System.out.println(event.toString());
//					
				if (event.getLinkId().toString().equals("link2")){
					Assert.assertEquals("wrong link enter time (link2).", 101.0, event.getTime(), MatsimTestUtils.EPSILON);
				} else if (event.getLinkId().toString().equals("link3")){
					Assert.assertEquals("wrong link enter time (link3).", 103.0, event.getTime(), MatsimTestUtils.EPSILON);
				} else if (event.getLinkId().toString().equals("link4")){
					Assert.assertEquals("wrong link enter time (link4).", 104.0, event.getTime(), MatsimTestUtils.EPSILON);
				} else if (event.getLinkId().toString().equals("link5")){
					Assert.assertEquals("wrong link enter time (link5).", 106.0, event.getTime(), MatsimTestUtils.EPSILON);
				}
			}			
	}
	
	private void setPopulation(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity workActLink5 = popFactory.createActivityFromLinkId("work", linkId5);
		
		Leg leg_0_5 = popFactory.createLeg("car");
		List<Id<Link>> linkIds_0_5 = new ArrayList<Id<Link>>();
		linkIds_0_5.add(linkId2);
		linkIds_0_5.add(linkId3);
		linkIds_0_5.add(linkId4);
		
		NetworkRoute route_0_5 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
		route_0_5.setLinkIds(linkId1, linkIds_0_5, linkId5);
		leg_0_5.setRoute(route_0_5);
		
		// ################################################################
		// test agent
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity homeActLink1_1 = popFactory.createActivityFromLinkId("home", linkId1);
		homeActLink1_1.setEndTime(100);
		plan1.addActivity(homeActLink1_1);
		plan1.addLeg(leg_0_5);
		plan1.addActivity(workActLink5);
		person1.addPlan(plan1);
		population.addPerson(person1);

	}
	
	private Scenario loadScenario() {
			
		// (0)		     (1)	       (2)		     (3)		   (4)	   	     (5)		
		//    ---link1---   ---link2---   ---link3---   ---link4---   ---link5--- 
		
		Config config = testUtils.loadConfig(null);
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		qSimConfigGroup.setRemoveStuckVehicles(true);
		qSimConfigGroup.setStuckTime(3600.0);
		Scenario scenario = (ScenarioUtils.createScenario(config));
	
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setEffectiveCellSize(7.5);
		network.setCapacityPeriod(3600.);
		
		Node node0 = network.getFactory().createNode(Id.create("0", Node.class), scenario.createCoord(0., 0.));
		Node node1 = network.getFactory().createNode(Id.create("1", Node.class), scenario.createCoord(100., 0.));
		Node node2 = network.getFactory().createNode(Id.create("2", Node.class), scenario.createCoord(200., 0.));
		Node node3 = network.getFactory().createNode(Id.create("3", Node.class), scenario.createCoord(300., 0.));
		Node node4 = network.getFactory().createNode(Id.create("4", Node.class), scenario.createCoord(400., 0.));
		Node node5 = network.getFactory().createNode(Id.create("5", Node.class), scenario.createCoord(500., 0.));
		
		Link link1 = network.getFactory().createLink(this.linkId1, node0, node1);
		Link link2 = network.getFactory().createLink(this.linkId2, node1, node2);
		Link link3 = network.getFactory().createLink(this.linkId3, node2, node3);
		Link link4 = network.getFactory().createLink(this.linkId4, node3, node4);
		Link link5 = network.getFactory().createLink(this.linkId5, node4, node5);

		Set<String> modes = new HashSet<String>();
		modes.add("car");
		
		link1.setAllowedModes(modes);
		link1.setCapacity(999999);
		link1.setFreespeed(1);
		link1.setNumberOfLanes(1000);
		link1.setLength(1);
		
		// t = s/v = 10/10 = 1 --> 1 time step on link
		link2.setAllowedModes(modes);
		link2.setCapacity(999999);
		link2.setFreespeed(10);
		link2.setNumberOfLanes(1000);
		link2.setLength(10);
		
		// t = s/v = 10/10.1 = 0.99 --> 0 time step on link
		link3.setAllowedModes(modes);
		link3.setCapacity(999999);
		link3.setFreespeed(10.1);
		link3.setNumberOfLanes(1000);
		link3.setLength(10);
		
		// t = s/v = 10/9.9 = 1.01 --> 1 time step on link
		link4.setAllowedModes(modes);
		link4.setCapacity(999999);
		link4.setFreespeed(9.9);
		link4.setNumberOfLanes(1000);
		link4.setLength(10);
		
		link5.setAllowedModes(modes);
		link5.setCapacity(999999);
		link5.setFreespeed(1);
		link5.setNumberOfLanes(1000);
		link5.setLength(1);
		
		network.addNode(node0);
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);
		
		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
		network.addLink(link4);
		network.addLink(link5);
		
		this.events = EventsUtils.createEventsManager();
		return scenario;
	}
	
	private QSim createQSim(Scenario sc, EventsManager events) {
		QSim qSim1 = new QSim(sc, events);
		ActivityEngine activityEngine = new ActivityEngine();
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);
        QNetsimEngineModule.configure(qSim1);
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim1.addMobsimEngine(teleportationEngine);
		QSim qSim = qSim1;
		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}
	
}
