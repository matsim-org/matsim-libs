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
package playground.ikaddoura.internalizationCar;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
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
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import playground.ikaddoura.internalizationCar.old.MarginalCongestionHandlerV1;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author ikaddoura
 *
 */
public class MarginalCongestionHandlerV1QsimTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	private EventsManager events;
	
	private Id testAgent1 = new IdImpl("testAgent1");
	private Id testAgent2 = new IdImpl("testAgent2");
	private Id testAgent3 = new IdImpl("testAgent3");
	private Id testAgent4 = new IdImpl("testAgent4");
	
	private Id linkId1 = new IdImpl("link1");
	private Id linkId2 = new IdImpl("link2");
	private Id linkId3 = new IdImpl("link3");
	private Id linkId4 = new IdImpl("link4");
	
	@Test
	public final void testFlowCongestion_4agents(){
		
		Scenario sc = loadScenario1();
		setPopulation1(sc);
		
		final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
		
		events.addHandler( new MarginalCongestionEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(MarginalCongestionEvent event) {
				congestionEvents.add(event);
			}
			
		});
		
		events.addHandler(new MarginalCongestionHandlerV1(events, (ScenarioImpl) sc));
				
		QSim sim = createQSim(sc, events);
		sim.run();
		
		for (MarginalCongestionEvent event : congestionEvents) {
		
			if (event.getTime() == 160.0) {
				
				if (event.getCausingAgentId().toString().equals(this.testAgent2.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent3.toString())) {
					Assert.assertEquals("wrong delay.", 9.0, event.getDelay(), MatsimTestUtils.EPSILON);
				
				} else if (event.getCausingAgentId().toString().equals(this.testAgent1.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent2.toString())) {
					Assert.assertEquals("wrong delay.", 58.0, event.getDelay(), MatsimTestUtils.EPSILON);
				} 
				
			} else if (event.getTime() == 170.0) {
				
				if (event.getCausingAgentId().toString().equals(this.testAgent3.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent4.toString())) {
					Assert.assertEquals("wrong delay.", 10.0, event.getDelay(), MatsimTestUtils.EPSILON);
				
				} else if (event.getCausingAgentId().toString().equals(this.testAgent2.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent4.toString())) {
					Assert.assertEquals("wrong delay.", 8.0, event.getDelay(), MatsimTestUtils.EPSILON);
				} 
				
			}
		}
		
	}
	
	@Test
	public final void testFlowCongestion_3agents_sameTime(){
		
		testUtils.starting(new FrameworkMethod(MarginalCongestionHandlerV1QsimTest.class.getMethods()[0]));
		Scenario sc = loadScenario2();
		setPopulation2(sc);
		
		final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
		
		events.addHandler( new MarginalCongestionEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(MarginalCongestionEvent event) {
				congestionEvents.add(event);
			}
			
		});
		
		events.addHandler(new MarginalCongestionHandlerV1(events, (ScenarioImpl) sc));
				
		QSim sim = createQSim(sc, events);
		sim.run();
		
		Assert.assertEquals("wrong number of congestion events" , 3, congestionEvents.size());
		
		for (MarginalCongestionEvent event : congestionEvents) {
			
			if (event.getCausingAgentId().toString().equals(this.testAgent3.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent2.toString())) {
				Assert.assertEquals("wrong delay.", 10.0, event.getDelay(), MatsimTestUtils.EPSILON);
			
			} else if (event.getCausingAgentId().toString().equals(this.testAgent2.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent1.toString())) {
				Assert.assertEquals("wrong delay.", 10.0, event.getDelay(), MatsimTestUtils.EPSILON);
			
			} else if (event.getCausingAgentId().toString().equals(this.testAgent3.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent1.toString())) {
				Assert.assertEquals("wrong delay.", 10.0, event.getDelay(), MatsimTestUtils.EPSILON);
			}
		}
		
	}
	
	// letzter agent gerade noch so im Stau
	@Test
	public final void testFlowCongestion_2agents_differentTimes(){
		
		testUtils.starting(new FrameworkMethod(MarginalCongestionHandlerV1QsimTest.class.getMethods()[0]));
		Scenario sc = loadScenario2();
		setPopulation3(sc);
		
		final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
		
		events.addHandler( new MarginalCongestionEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(MarginalCongestionEvent event) {
				congestionEvents.add(event);
			}
			
		});
		
		events.addHandler(new MarginalCongestionHandlerV1(events, (ScenarioImpl) sc));
				
		QSim sim = createQSim(sc, events);
		sim.run();
				
		for (MarginalCongestionEvent event : congestionEvents) {
			if (event.getCausingAgentId().toString().equals(this.testAgent1.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent2.toString())) {
				Assert.assertEquals("wrong delay.", 1.0, event.getDelay(), MatsimTestUtils.EPSILON);
			}
		}
		Assert.assertEquals("wrong number of congestion events" , 1, congestionEvents.size());
		
	}
	
	// ################################################################################################################################

	private void setPopulation1(Scenario scenario) {
		
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
	
	private void setPopulation2(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity lastActLink3 = popFactory.createActivityFromLinkId("work", linkId3);
		
		// leg: 1,2,3
		Leg leg_1_3 = popFactory.createLeg("car");
		List<Id<Link>> linkIds3 = new ArrayList<Id<Link>>();
		linkIds3.add(linkId2);
		NetworkRoute route3 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route3.setLinkIds(linkId1, linkIds3, linkId3);
		leg_1_3.setRoute(route3);		
		
		// ################################################################
		// first agent (1 --> 3)
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromLinkId("home", linkId1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
		plan1.addLeg(leg_1_3);
		plan1.addActivity(lastActLink3);
		person1.addPlan(plan1);
		population.addPerson(person1);

		// ################################################################
		// second agent (1 --> 3)		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromLinkId("home", linkId1);
		act2.setEndTime(99);
		plan2.addActivity(act2);
		plan2.addLeg(leg_1_3);
		plan2.addActivity(lastActLink3);	
		person2.addPlan(plan2);
		population.addPerson(person2);
		
		// ################################################################
		// third agent (1 --> 3)		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromLinkId("home", linkId1);
		act3.setEndTime(99);
		plan3.addActivity(act3);
		plan3.addLeg(leg_1_3);
		plan3.addActivity(lastActLink3);	
		person3.addPlan(plan3);
		population.addPerson(person3);

	}
	
	private void setPopulation3(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity lastActLink3 = popFactory.createActivityFromLinkId("work", linkId3);
		
		// leg: 1,2,3
		Leg leg_1_3 = popFactory.createLeg("car");
		List<Id<Link>> linkIds3 = new ArrayList<Id<Link>>();
		linkIds3.add(linkId2);
		NetworkRoute route3 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route3.setLinkIds(linkId1, linkIds3, linkId3);
		leg_1_3.setRoute(route3);		
		
		// ################################################################
		// first agent (1 --> 3)
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromLinkId("home", linkId1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
		plan1.addLeg(leg_1_3);
		plan1.addActivity(lastActLink3);
		person1.addPlan(plan1);
		population.addPerson(person1);

		// ################################################################
		// second agent (1 --> 3)		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromLinkId("home", linkId1);
		act2.setEndTime(109);
		plan2.addActivity(act2);
		plan2.addLeg(leg_1_3);
		plan2.addActivity(lastActLink3);	
		person2.addPlan(plan2);
		population.addPerson(person2);

	}

	private Scenario loadScenario1() {
			
		// (0)-----link1-----(1)-----link2-----(2)-----link3-----(3)-----link4-----(4)
		
		Config config = testUtils.loadConfig(null);
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		Scenario scenario = (ScenarioUtils.createScenario(config));
	
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setEffectiveCellSize(7.5);
		network.setCapacityPeriod(3600.);
		
		Node node0 = network.getFactory().createNode(new IdImpl("0"), scenario.createCoord(0., 0.));
		Node node1 = network.getFactory().createNode(new IdImpl("1"), scenario.createCoord(100., 0.));
		Node node2 = network.getFactory().createNode(new IdImpl("2"), scenario.createCoord(200., 0.));
		Node node3 = network.getFactory().createNode(new IdImpl("3"), scenario.createCoord(300., 0.));
		Node node4 = network.getFactory().createNode(new IdImpl("4"), scenario.createCoord(400., 0.));
		
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
	
	private Scenario loadScenario2() {
		
		// (0)-----link1-----(1)-----link2-----(2)-----link3-----(3)-----link4-----(4)
		
		Config config = testUtils.loadConfig(null);
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		Scenario scenario = (ScenarioUtils.createScenario(config));
	
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setEffectiveCellSize(7.5);
		network.setCapacityPeriod(3600.);
		
		Node node0 = network.getFactory().createNode(new IdImpl("0"), scenario.createCoord(0., 0.));
		Node node1 = network.getFactory().createNode(new IdImpl("1"), scenario.createCoord(100., 0.));
		Node node2 = network.getFactory().createNode(new IdImpl("2"), scenario.createCoord(200., 0.));
		Node node3 = network.getFactory().createNode(new IdImpl("3"), scenario.createCoord(300., 0.));
		Node node4 = network.getFactory().createNode(new IdImpl("4"), scenario.createCoord(400., 0.));
		
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
		
		// link without capacity restrictions
		link3.setAllowedModes(modes);
		link3.setCapacity(10800);
		link3.setFreespeed(500);
		link3.setNumberOfLanes(100);
		link3.setLength(500);
		
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
