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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author ikaddoura
 *
 */

public class MarginalCongestionHandlerV2QsimTestStorage {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	private EventsManager events;
	
	private Id testAgent1 = new IdImpl("testAgent1");
	private Id testAgent2 = new IdImpl("testAgent2");
	private Id testAgent3 = new IdImpl("testAgent3");
	private Id testAgent4 = new IdImpl("testAgent4");
	private Id testAgent5 = new IdImpl("testAgent5");
	private Id testAgent6 = new IdImpl("testAgent6");
	private Id testAgent7 = new IdImpl("testAgent7");
	private Id testAgent8 = new IdImpl("testAgent8");
	private Id testAgent9 = new IdImpl("testAgent9");
	private Id testAgent10 = new IdImpl("testAgent10");
	
	private Id linkId1 = new IdImpl("link1");
	private Id linkId2 = new IdImpl("link2");
	private Id linkId3 = new IdImpl("link3");
	private Id linkId4 = new IdImpl("link4");
	private Id linkId5 = new IdImpl("link5");
	private Id linkId6 = new IdImpl("link6");
	
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

		// eight agents start with a gap of each one second, a ninth agent starts a bit later
		// at a node (no flow-restrictions), all agents go the same way, the storage-capacity of 5 vehicles on that link is reached,
		// the ninth agent has to wait 15 seconds to enter the bottleneck-link due to storage constraints (more than the flow-capacity of the next link) 
		// the previous agent (the eighth agent has to pay for this delay)
		@Test
		public final void testStorageCapacity1a(){
				
			testUtils.starting(new FrameworkMethod(MarginalCongestionHandlerV2QsimTest.class.getMethods()[0]));
			Scenario sc = loadScenarioStorage1();
			setPopulationStorage1a(sc);
				
			final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
			
			events.addHandler( new LinkEnterEventHandler() {
				
				@Override
				public void reset(int iteration) {
				}
				
				@Override
				public void handleEvent(LinkEnterEvent event) {
					if(event.getPersonId().toString().equals(testAgent9.toString())){
						System.out.println(event.toString());
					}
					if(event.getPersonId().toString().equals(testAgent8.toString())){
						System.out.println(event.toString());
					}
				}
			});
			
			events.addHandler( new MarginalCongestionEventHandler() {

				@Override
				public void reset(int iteration) {				
				}

				@Override
				public void handleEvent(MarginalCongestionEvent event) {
					congestionEvents.add(event);
					if(event.getAffectedAgentId().toString().equals(testAgent9.toString())){
						System.out.println(event.toString());
					}
				}
					
			});
			
			events.addHandler(new MarginalCongestionHandlerImplV2(events, (ScenarioImpl) sc));
						
			QSim sim = createQSim(sc, events);
			sim.run();
						
			for (MarginalCongestionEvent event : congestionEvents) {
				
				if (event.getTime() == 198.0) {
					if (event.getCausingAgentId().toString().equals(this.testAgent8.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent9.toString()) && event.getCapacityConstraint().toString().equals("storageCapacity")) {
						Assert.assertEquals("wrong delay.", 15.0, event.getDelay(), MatsimTestUtils.EPSILON);
					}else{}
				
				}else{}
			
			}
			
		}
		
		// eigth agents start with a gap of each one second, a ninth and a tenth agent start a bit later with a gap of 1 second
		// at a node (no flow-restrictions), all agents go the same way, the storage-capacity of 5 vehicles on that link is reached,
		// the ninth agent has to wait 15 seconds to enter the next link (in both cases due to storage constraints)
		// the previous agent (at first the eighth agent has to pay for the delay of 15 seconds, then the ninth agent has to pay for the delay of 24 seconds even if he has not been there 25 seconds before)
		@Test
		public final void testStorageCapacity2a(){
			testUtils.starting(new FrameworkMethod(MarginalCongestionHandlerV2QsimTest.class.getMethods()[0]));
			Scenario sc = loadScenarioStorage2();
			setPopulationStorage2a(sc);
				
			final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
			
			events.addHandler( new LinkEnterEventHandler() {
				
				@Override
				public void reset(int iteration) {
				}
				
				@Override
				public void handleEvent(LinkEnterEvent event) {
					if(event.getPersonId().toString().equals(testAgent10.toString())){
						System.out.println(event.toString());
					}
					if(event.getPersonId().toString().equals(testAgent9.toString())){
						System.out.println(event.toString());
					}
					if(event.getPersonId().toString().equals(testAgent8.toString())){
						System.out.println(event.toString());
					}
				}
			});
			
			events.addHandler( new MarginalCongestionEventHandler() {

				@Override
				public void reset(int iteration) {				
				}

				@Override
				public void handleEvent(MarginalCongestionEvent event) {
					congestionEvents.add(event);
					if(event.getAffectedAgentId().toString().equals(testAgent10.toString())){
						System.out.println(event.toString());
					}
					if(event.getAffectedAgentId().toString().equals(testAgent9.toString())){
						System.out.println(event.toString());
					}
				}
					
			});
			
			events.addHandler(new MarginalCongestionHandlerImplV2(events, (ScenarioImpl) sc));
						
			QSim sim = createQSim(sc, events);
			sim.run();
						
			for (MarginalCongestionEvent event : congestionEvents) {
				
				if (event.getTime() == 198.0) {
					if (event.getCausingAgentId().toString().equals(this.testAgent8.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent9.toString()) && event.getCapacityConstraint().toString().equals("storageCapacity")) {
						Assert.assertEquals("wrong delay.", 15.0, event.getDelay(), MatsimTestUtils.EPSILON);
					}else{}
				
				}else if (event.getTime() == 208.0) {
					if (event.getCausingAgentId().toString().equals(this.testAgent9.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent10.toString()) && event.getCapacityConstraint().toString().equals("storageCapacity")) {
						Assert.assertEquals("wrong delay.", 24.0, event.getDelay(), MatsimTestUtils.EPSILON);
					}else{}
				}
			}
		}
		
		// eigth agents start with a gap of each one second on link 1 and go on the same way to the destination link 6, a ninth agent start a bit later on link 3 and has the same destination link
		// at a node (no flow-restrictions), the first eight agents go the same way, the storage-capacity of 5 vehicles on that link is reached,
		// the ninth agent has to wait 15 seconds to enter the next link
		// who has to pay the delay for the ninth agent?!
		// there is no agent who has left link 4 before.
		@Ignore
		@Test
		public final void testStorageCapacity2b(){
			testUtils.starting(new FrameworkMethod(MarginalCongestionHandlerV2QsimTest.class.getMethods()[0]));
			Scenario sc = loadScenarioStorage2();
			setPopulationStorage2b(sc);
				
			final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
			
			events.addHandler( new LinkEnterEventHandler() {
				
				@Override
				public void reset(int iteration) {
				}
				
				@Override
				public void handleEvent(LinkEnterEvent event) {
					if(event.getPersonId().toString().equals(testAgent9.toString())){
						System.out.println(event.toString());
					}
					if(event.getPersonId().toString().equals(testAgent8.toString())){
						System.out.println(event.toString());
					}
				}
			});
			
			events.addHandler( new MarginalCongestionEventHandler() {

				@Override
				public void reset(int iteration) {				
				}

				@Override
				public void handleEvent(MarginalCongestionEvent event) {
					congestionEvents.add(event);
					if(event.getAffectedAgentId().toString().equals(testAgent9.toString())){
						System.out.println(event.toString());
					}
					System.out.println(event.toString());
				}
					
			});
			
			events.addHandler(new MarginalCongestionHandlerImplV2(events, (ScenarioImpl) sc));
						
			QSim sim = createQSim(sc, events);
			sim.run();
						
			for (MarginalCongestionEvent event : congestionEvents) {
				
				if (event.getTime() == 198.0) {
					if (event.getCausingAgentId().toString().equals(this.testAgent8.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent9.toString()) && event.getCapacityConstraint().toString().equals("storageCapacity")) {
						Assert.assertEquals("wrong delay.", 15.0, event.getDelay(), MatsimTestUtils.EPSILON);
					}else{}
				
				}else{}
			}
		}
	
	// ################################################################################################################################

	private void setPopulationStorage1a(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity lastActLink4 = popFactory.createActivityFromLinkId("work", linkId4);
	
		// leg: 1,2,3,4
		Leg leg_1_4 = popFactory.createLeg("car");
		List<Id<Link>> linkIds4 = new ArrayList<Id<Link>>();
		linkIds4.add(linkId2);
		linkIds4.add(linkId3);
		NetworkRoute route4 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route4.setLinkIds(linkId1, linkIds4, linkId4);
		leg_1_4.setRoute(route4);		
		
		// ################################################################
		// first agent (1 --> 4)
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromLinkId("home", linkId1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
		plan1.addLeg(leg_1_4);
		plan1.addActivity(lastActLink4);
		person1.addPlan(plan1);
		population.addPerson(person1);

		// ################################################################
		// second agent (1 --> 4)		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromLinkId("home", linkId1);
		act2.setEndTime(100);
		plan2.addActivity(act2);
		plan2.addLeg(leg_1_4);
		plan2.addActivity(lastActLink4);	
		person2.addPlan(plan2);
		population.addPerson(person2);
		
		// ################################################################
		// third agent (1 --> 4)		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromLinkId("home", linkId1);
		act3.setEndTime(101);
		plan3.addActivity(act3);
		plan3.addLeg(leg_1_4);
		plan3.addActivity(lastActLink4);	
		person3.addPlan(plan3);
		population.addPerson(person3);
		
		// ################################################################
		// fourth agent (1 --> 4)		
		Person person4 = popFactory.createPerson(testAgent4);
		Plan plan4 = popFactory.createPlan();
		Activity act4 = popFactory.createActivityFromLinkId("home", linkId1);				
		act4.setEndTime(102);
		plan4.addActivity(act4);
		plan4.addLeg(leg_1_4);
		plan4.addActivity(lastActLink4);	
		person4.addPlan(plan4);
		population.addPerson(person4);
		
		// ################################################################
		// fifth agent (1 --> 4)		
		Person person5 = popFactory.createPerson(testAgent5);
		Plan plan5 = popFactory.createPlan();
		Activity act5 = popFactory.createActivityFromLinkId("home", linkId1);				
		act5.setEndTime(103);
		plan5.addActivity(act5);
		plan5.addLeg(leg_1_4);
		plan5.addActivity(lastActLink4);	
		person5.addPlan(plan5);
		population.addPerson(person5);
				
		// ################################################################
		// sixth agent (1 --> 4)		
		Person person6 = popFactory.createPerson(testAgent6);
		Plan plan6 = popFactory.createPlan();
		Activity act6 = popFactory.createActivityFromLinkId("home", linkId1);				
		act6.setEndTime(104);
		plan6.addActivity(act6);
		plan6.addLeg(leg_1_4);
		plan6.addActivity(lastActLink4);	
		person6.addPlan(plan6);
		population.addPerson(person6);
				
		// ################################################################
		// seventh agent (1 --> 4)		
		Person person7 = popFactory.createPerson(testAgent7);
		Plan plan7 = popFactory.createPlan();
		Activity act7 = popFactory.createActivityFromLinkId("home", linkId1);				
		act7.setEndTime(105);
		plan7.addActivity(act7);
		plan7.addLeg(leg_1_4);
		plan7.addActivity(lastActLink4);	
		person7.addPlan(plan7);
		population.addPerson(person7);
				
		// ################################################################
		// eighth agent (1 --> 4)		
		Person person8 = popFactory.createPerson(testAgent8);
		Plan plan8 = popFactory.createPlan();
		Activity act8 = popFactory.createActivityFromLinkId("home", linkId1);				
		act8.setEndTime(106);
		plan8.addActivity(act8);
		plan8.addLeg(leg_1_4);
		plan8.addActivity(lastActLink4);	
		person8.addPlan(plan8);
		population.addPerson(person8);
		
		// ################################################################
		// ninth agent (1 --> 4)		
		Person person9 = popFactory.createPerson(testAgent9);
		Plan plan9 = popFactory.createPlan();
		Activity act9 = popFactory.createActivityFromLinkId("home", linkId1);				
		act9.setEndTime(131);
		plan9.addActivity(act9);
		plan9.addLeg(leg_1_4);
		plan9.addActivity(lastActLink4);	
		person9.addPlan(plan9);
		population.addPerson(person9);
		
		// ################################################################
	
	}

	private void setPopulationStorage1b(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity lastActLink4 = popFactory.createActivityFromLinkId("work", linkId4);
		Activity lastActLink6 = popFactory.createActivityFromLinkId("work", linkId6);
	
		// leg: 1,2,3,4
		Leg leg_1_4 = popFactory.createLeg("car");
		List<Id<Link>> linkIds4 = new ArrayList<Id<Link>>();
		linkIds4.add(linkId2);
		linkIds4.add(linkId3);
		NetworkRoute route4 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route4.setLinkIds(linkId1, linkIds4, linkId4);
		leg_1_4.setRoute(route4);

		// leg: 1,2,5,6
		Leg leg_1_6 = popFactory.createLeg("car");
		List<Id<Link>> linkIds6 = new ArrayList<Id<Link>>();
		linkIds6.add(linkId2);
		linkIds6.add(linkId5);
		NetworkRoute route6 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		route6.setLinkIds(linkId1, linkIds6, linkId6);
		leg_1_6.setRoute(route6);			
				
		// ################################################################
		// first agent (1 --> 4)
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromLinkId("home", linkId1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
		plan1.addLeg(leg_1_4);
		plan1.addActivity(lastActLink4);
		person1.addPlan(plan1);
		population.addPerson(person1);

		// ################################################################
		// second agent (1 --> 4)		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();				
		Activity act2 = popFactory.createActivityFromLinkId("home", linkId1);				
		act2.setEndTime(100);		
		plan2.addActivity(act2);
		plan2.addLeg(leg_1_4);
		plan2.addActivity(lastActLink4);			
		person2.addPlan(plan2);
		population.addPerson(person2);
				
		// ################################################################
		// third agent (1 --> 4)		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromLinkId("home", linkId1);
		act3.setEndTime(101);
		plan3.addActivity(act3);
		plan3.addLeg(leg_1_4);
		plan3.addActivity(lastActLink4);	
		person3.addPlan(plan3);
		population.addPerson(person3);
				
		// ################################################################
		// fourth agent (1 --> 4)		
		Person person4 = popFactory.createPerson(testAgent4);
		Plan plan4 = popFactory.createPlan();
		Activity act4 = popFactory.createActivityFromLinkId("home", linkId1);				
		act4.setEndTime(102);
		plan4.addActivity(act4);
		plan4.addLeg(leg_1_4);
		plan4.addActivity(lastActLink4);	
		person4.addPlan(plan4);
		population.addPerson(person4);
				
		// ################################################################
		// fifth agent (1 --> 4)		
		Person person5 = popFactory.createPerson(testAgent5);
		Plan plan5 = popFactory.createPlan();
		Activity act5 = popFactory.createActivityFromLinkId("home", linkId1);				
		act5.setEndTime(103);
		plan5.addActivity(act5);
		plan5.addLeg(leg_1_4);
		plan5.addActivity(lastActLink4);	
		person5.addPlan(plan5);
		population.addPerson(person5);
						
		// ################################################################
		// sixth agent (1 --> 4)		
		Person person6 = popFactory.createPerson(testAgent6);
		Plan plan6 = popFactory.createPlan();
		Activity act6 = popFactory.createActivityFromLinkId("home", linkId1);				
		act6.setEndTime(104);
		plan6.addActivity(act6);
		plan6.addLeg(leg_1_4);
		plan6.addActivity(lastActLink4);	
		person6.addPlan(plan6);
		population.addPerson(person6);
						
		// ################################################################
		// seventh agent (1 --> 4)		
		Person person7 = popFactory.createPerson(testAgent7);
		Plan plan7 = popFactory.createPlan();
		Activity act7 = popFactory.createActivityFromLinkId("home", linkId1);				
		act7.setEndTime(105);
		plan7.addActivity(act7);
		plan7.addLeg(leg_1_4);
		plan7.addActivity(lastActLink4);	
		person7.addPlan(plan7);
		population.addPerson(person7);
						
		// ################################################################
		// eighth agent (1 --> 4)		
		Person person8 = popFactory.createPerson(testAgent8);
		Plan plan8 = popFactory.createPlan();
		Activity act8 = popFactory.createActivityFromLinkId("home", linkId1);				
		act8.setEndTime(106);
		plan8.addActivity(act8);
		plan8.addLeg(leg_1_4);
		plan8.addActivity(lastActLink4);	
		person8.addPlan(plan8);
		population.addPerson(person8);
				
		// ################################################################
		// ninth agent (1 --> 4)		
		Person person9 = popFactory.createPerson(testAgent9);
		Plan plan9 = popFactory.createPlan();
		Activity act9 = popFactory.createActivityFromLinkId("home", linkId1);				
		act9.setEndTime(131);
		plan9.addActivity(act9);
		plan9.addLeg(leg_1_6);
		plan9.addActivity(lastActLink6);	
		person9.addPlan(plan9);
		population.addPerson(person9);
				
		// ################################################################
			
	}
	
	private void setPopulationStorage2a(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity lastActLink6 = popFactory.createActivityFromLinkId("work", linkId6);
	
		// leg: 1,2,5,6
		Leg leg_1_6 = popFactory.createLeg("car");
		List<Id<Link>> linkIds6a = new ArrayList<Id<Link>>();
		linkIds6a.add(linkId2);
		linkIds6a.add(linkId5);
		NetworkRoute route6a = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		route6a.setLinkIds(linkId1, linkIds6a, linkId6);
		leg_1_6.setRoute(route6a);		
		
		// ################################################################
		// first agent (1 --> 6)
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromLinkId("home", linkId1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
		plan1.addLeg(leg_1_6);
		plan1.addActivity(lastActLink6);
		person1.addPlan(plan1);
		population.addPerson(person1);

		// ################################################################
		// second agent (1 --> 6)		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromLinkId("home", linkId1);
		act2.setEndTime(100);
		plan2.addActivity(act2);
		plan2.addLeg(leg_1_6);
		plan2.addActivity(lastActLink6);	
		person2.addPlan(plan2);
		population.addPerson(person2);
		
		// ################################################################
		// third agent (1 --> 6)		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromLinkId("home", linkId1);
		act3.setEndTime(101);
		plan3.addActivity(act3);
		plan3.addLeg(leg_1_6);
		plan3.addActivity(lastActLink6);	
		person3.addPlan(plan3);
		population.addPerson(person3);
		
		// ################################################################
		// fourth agent (1 --> 6)		
		Person person4 = popFactory.createPerson(testAgent4);
		Plan plan4 = popFactory.createPlan();
		Activity act4 = popFactory.createActivityFromLinkId("home", linkId1);				
		act4.setEndTime(102);
		plan4.addActivity(act4);
		plan4.addLeg(leg_1_6);
		plan4.addActivity(lastActLink6);	
		person4.addPlan(plan4);
		population.addPerson(person4);
		
		// ################################################################
		// fifth agent (1 --> 6)		
		Person person5 = popFactory.createPerson(testAgent5);
		Plan plan5 = popFactory.createPlan();
		Activity act5 = popFactory.createActivityFromLinkId("home", linkId1);				
		act5.setEndTime(103);
		plan5.addActivity(act5);
		plan5.addLeg(leg_1_6);
		plan5.addActivity(lastActLink6);	
		person5.addPlan(plan5);
		population.addPerson(person5);
		
		// ################################################################
		// sixth agent (1 --> 6)		
		Person person6 = popFactory.createPerson(testAgent6);
		Plan plan6 = popFactory.createPlan();
		Activity act6 = popFactory.createActivityFromLinkId("home", linkId1);				
		act6.setEndTime(104);
		plan6.addActivity(act6);
		plan6.addLeg(leg_1_6);
		plan6.addActivity(lastActLink6);	
		person6.addPlan(plan6);
		population.addPerson(person6);
				
		// ################################################################
		// seventh agent (1 --> 6)		
		Person person7 = popFactory.createPerson(testAgent7);
		Plan plan7 = popFactory.createPlan();
		Activity act7 = popFactory.createActivityFromLinkId("home", linkId1);				
		act7.setEndTime(105);
		plan7.addActivity(act7);
		plan7.addLeg(leg_1_6);
		plan7.addActivity(lastActLink6);	
		person7.addPlan(plan7);
		population.addPerson(person7);
				
		// ################################################################
		// eighth agent (1 --> 6)		
		Person person8 = popFactory.createPerson(testAgent8);
		Plan plan8 = popFactory.createPlan();
		Activity act8 = popFactory.createActivityFromLinkId("home", linkId1);				
		act8.setEndTime(106);
		plan8.addActivity(act8);
		plan8.addLeg(leg_1_6);
		plan8.addActivity(lastActLink6);	
		person8.addPlan(plan8);
		population.addPerson(person8);
				
		// ################################################################
		// ninth agent (1 --> 6)		
		Person person9 = popFactory.createPerson(testAgent9);
		Plan plan9 = popFactory.createPlan();
		Activity act9 = popFactory.createActivityFromLinkId("home", linkId1);				
		act9.setEndTime(131);
		plan9.addActivity(act9);
		plan9.addLeg(leg_1_6);
		plan9.addActivity(lastActLink6);	
		person9.addPlan(plan9);
		population.addPerson(person9);
				
		// ################################################################
		// tenth agent (1 --> 6)		
		Person person10 = popFactory.createPerson(testAgent10);
		Plan plan10 = popFactory.createPlan();
		Activity act10 = popFactory.createActivityFromLinkId("home", linkId1);				
		act10.setEndTime(132);
		plan10.addActivity(act10);
		plan10.addLeg(leg_1_6);
		plan10.addActivity(lastActLink6);	
		person10.addPlan(plan10);
		population.addPerson(person10);
	
	}

	private void setPopulationStorage2b(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity lastActLink6 = popFactory.createActivityFromLinkId("work", linkId6);
	
		// leg: 1,2,5,6
		Leg leg_1_6 = popFactory.createLeg("car");
		List<Id<Link>> linkIds6a = new ArrayList<Id<Link>>();
		linkIds6a.add(linkId2);
		linkIds6a.add(linkId5);
		NetworkRoute route6a = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		route6a.setLinkIds(linkId1, linkIds6a, linkId6);
		leg_1_6.setRoute(route6a);

		// leg: 3,4,5,6
		Leg leg_3_6 = popFactory.createLeg("car");
		List<Id<Link>> linkIds6b = new ArrayList<Id<Link>>();
		linkIds6b.add(linkId4);
		linkIds6b.add(linkId5);
		NetworkRoute route6b = (NetworkRoute) routeFactory.createRoute(linkId3, linkId6);
		route6b.setLinkIds(linkId3, linkIds6b, linkId6);
		leg_3_6.setRoute(route6b);		
		
		// ################################################################				
		// first agent (1 --> 6)		
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();		
		Activity act1 = popFactory.createActivityFromLinkId("home", linkId1);		
		act1.setEndTime(99);		
		plan1.addActivity(act1);		
		plan1.addLeg(leg_1_6);		
		plan1.addActivity(lastActLink6);		
		person1.addPlan(plan1);
		population.addPerson(person1);
		
		// ###############################################################		
		// second agent (1 --> 6)				
		Person person2 = popFactory.createPerson(testAgent2);		
		Plan plan2 = popFactory.createPlan();		
		Activity act2 = popFactory.createActivityFromLinkId("home", linkId1);
		act2.setEndTime(100);
		plan2.addActivity(act2);
		plan2.addLeg(leg_1_6);
		plan2.addActivity(lastActLink6);	
		person2.addPlan(plan2);
		population.addPerson(person2);
	
		// ################################################################	
		// third agent (1 --> 6)		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromLinkId("home", linkId1);
		act3.setEndTime(101);
		plan3.addActivity(act3);
		plan3.addLeg(leg_1_6);
		plan3.addActivity(lastActLink6);	
		person3.addPlan(plan3);
		population.addPerson(person3);
		
		// ################################################################
		// fourth agent (1 --> 6)		
		Person person4 = popFactory.createPerson(testAgent4);
		Plan plan4 = popFactory.createPlan();
		Activity act4 = popFactory.createActivityFromLinkId("home", linkId1);				
		act4.setEndTime(102);
		plan4.addActivity(act4);
		plan4.addLeg(leg_1_6);
		plan4.addActivity(lastActLink6);	
		person4.addPlan(plan4);
		population.addPerson(person4);
		
		// ################################################################
		// fifth agent (1 --> 6)		
		Person person5 = popFactory.createPerson(testAgent5);
		Plan plan5 = popFactory.createPlan();
		Activity act5 = popFactory.createActivityFromLinkId("home", linkId1);				
		act5.setEndTime(103);
		plan5.addActivity(act5);
		plan5.addLeg(leg_1_6);
		plan5.addActivity(lastActLink6);	
		person5.addPlan(plan5);
		population.addPerson(person5);
	
		// ################################################################	
		// sixth agent (1 --> 6)		
		Person person6 = popFactory.createPerson(testAgent6);
		Plan plan6 = popFactory.createPlan();
		Activity act6 = popFactory.createActivityFromLinkId("home", linkId1);				
		act6.setEndTime(104);
		plan6.addActivity(act6);
		plan6.addLeg(leg_1_6);
		plan6.addActivity(lastActLink6);	
		person6.addPlan(plan6);
		population.addPerson(person6);
		
		// ################################################################
		// seventh agent (1 --> 6)		
		Person person7 = popFactory.createPerson(testAgent7);
		Plan plan7 = popFactory.createPlan();
		Activity act7 = popFactory.createActivityFromLinkId("home", linkId1);				
		act7.setEndTime(105);
		plan7.addActivity(act7);
		plan7.addLeg(leg_1_6);
		plan7.addActivity(lastActLink6);	
		person7.addPlan(plan7);
		population.addPerson(person7);
		
		// ################################################################
		// eighth agent (1 --> 6)	
		Person person8 = popFactory.createPerson(testAgent8);
		Plan plan8 = popFactory.createPlan();
		Activity act8 = popFactory.createActivityFromLinkId("home", linkId1);				
		act8.setEndTime(106);
		plan8.addActivity(act8);
		plan8.addLeg(leg_1_6);
		plan8.addActivity(lastActLink6);	
		person8.addPlan(plan8);
		population.addPerson(person8);
		
		// ################################################################
		// ninth agent (1 --> 6)		
		Person person9 = popFactory.createPerson(testAgent9);
		Plan plan9 = popFactory.createPlan();		
		Activity act9 = popFactory.createActivityFromLinkId("home", linkId3);				
		act9.setEndTime(131);
		plan9.addActivity(act9);				
		plan9.addLeg(leg_3_6);
		plan9.addActivity(lastActLink6);	
		person9.addPlan(plan9);
		population.addPerson(person9);
						
	}
	
	private Scenario loadScenarioStorage1() {
	
		//					       					(3)-----link4-----(4)
		//					      					 /
		//					   					    /
		//					   					 link3
		//					    				  /
		//					  					 /
		// (0)-----link1-----(1)-----link2-----(2)
		//					  					 \
		//					  					  \
		//					  					  link5
		//					    		     	    \
		//					    				     \
		//					    				     (5)-----link6-----(6)
		
		Config config = testUtils.loadConfig(null);
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		qSimConfigGroup.setRemoveStuckVehicles(true);
		qSimConfigGroup.setStuckTime(100.0);
		Scenario scenario = (ScenarioUtils.createScenario(config));
	
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setEffectiveCellSize(7.5);
		network.setCapacityPeriod(3600.);
		
		Node node0 = network.getFactory().createNode(new IdImpl("0"), scenario.createCoord(0., 0.));
		Node node1 = network.getFactory().createNode(new IdImpl("1"), scenario.createCoord(100., 0.));
		Node node2 = network.getFactory().createNode(new IdImpl("2"), scenario.createCoord(200., 0.));
		Node node3 = network.getFactory().createNode(new IdImpl("3"), scenario.createCoord(300., 100.));
		Node node4 = network.getFactory().createNode(new IdImpl("4"), scenario.createCoord(400., 100.));
		Node node5 = network.getFactory().createNode(new IdImpl("5"), scenario.createCoord(300., -100.));
		Node node6 = network.getFactory().createNode(new IdImpl("6"), scenario.createCoord(400., -100.));
		
		Link link1 = network.getFactory().createLink(this.linkId1, node0, node1);
		Link link2 = network.getFactory().createLink(this.linkId2, node1, node2);
		Link link3 = network.getFactory().createLink(this.linkId3, node2, node3);
		Link link4 = network.getFactory().createLink(this.linkId4, node3, node4);
		Link link5 = network.getFactory().createLink(this.linkId5, node2, node5);
		Link link6 = network.getFactory().createLink(this.linkId6, node5, node6);
	
		Set<String> modes = new HashSet<String>();
		modes.add("car");
		
		// link without capacity restrictions
		link1.setAllowedModes(modes);
		link1.setCapacity(36000);
		link1.setFreespeed(500);
		link1.setNumberOfLanes(100);
		link1.setLength(500);
		
		// link without capacity restrictions
		link2.setAllowedModes(modes);
		link2.setCapacity(36000);
		link2.setFreespeed(10);
		link2.setNumberOfLanes(100);
		link2.setLength(500);
		
		// link with storage (3 vehicles per link) and flow (1 vehicle / 10 seconds) restrictions
		link3.setAllowedModes(modes);
		link3.setCapacity(360);
		link3.setFreespeed(2.5);
		link3.setNumberOfLanes(1);
		link3.setLength(37.5);
		
		// link without capacity restrictions
		link4.setAllowedModes(modes);
		link4.setCapacity(36000);
		link4.setFreespeed(10);
		link4.setNumberOfLanes(100);
		link4.setLength(500);

		// link without capacity restrictions
		link5.setAllowedModes(modes);
		link5.setCapacity(36000);
		link5.setFreespeed(500);
		link5.setNumberOfLanes(100);
		link5.setLength(500);

		// link without capacity restrictions
		link6.setAllowedModes(modes);
		link6.setCapacity(36000);
		link6.setFreespeed(10);
		link6.setNumberOfLanes(100);
		link6.setLength(500);
		
		network.addNode(node0);
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);
		network.addNode(node6);
	
		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
		network.addLink(link4);
		network.addLink(link5);
		network.addLink(link6);
	
		this.events = EventsUtils.createEventsManager();
		return scenario;
	}

	private Scenario loadScenarioStorage2() {
	
		//	(0)-----link1-----(1)
		//					    \
		//					     \
		//					    link2
		//					       \
		//					        \		
		// 							(2)-----link5-----(5)-----link6-----(6)
		//					        /
		//					       /
		//					    link4
		//					     /
		//					    /
		//	(3)-----link3-----(4)		 
		
		Config config = testUtils.loadConfig(null);
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		qSimConfigGroup.setRemoveStuckVehicles(true);
		qSimConfigGroup.setStuckTime(100.0);
		Scenario scenario = (ScenarioUtils.createScenario(config));
	
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setEffectiveCellSize(7.5);
		network.setCapacityPeriod(3600.);
		
		Node node0 = network.getFactory().createNode(new IdImpl("0"), scenario.createCoord(0., 100.));
		Node node1 = network.getFactory().createNode(new IdImpl("1"), scenario.createCoord(100., 100.));
		Node node2 = network.getFactory().createNode(new IdImpl("2"), scenario.createCoord(200., 0.));
		Node node3 = network.getFactory().createNode(new IdImpl("3"), scenario.createCoord(0., -100.));
		Node node4 = network.getFactory().createNode(new IdImpl("4"), scenario.createCoord(0., -100.));
		Node node5 = network.getFactory().createNode(new IdImpl("5"), scenario.createCoord(300., 0.));
		Node node6 = network.getFactory().createNode(new IdImpl("6"), scenario.createCoord(400., 0.));
		
		Link link1 = network.getFactory().createLink(this.linkId1, node0, node1);
		Link link2 = network.getFactory().createLink(this.linkId2, node1, node2);
		Link link3 = network.getFactory().createLink(this.linkId3, node3, node4);
		Link link4 = network.getFactory().createLink(this.linkId4, node4, node2);
		Link link5 = network.getFactory().createLink(this.linkId5, node2, node5);
		Link link6 = network.getFactory().createLink(this.linkId6, node5, node6);
	
		Set<String> modes = new HashSet<String>();
		modes.add("car");
		
		// link without capacity restrictions
		link1.setAllowedModes(modes);
		link1.setCapacity(36000);
		link1.setFreespeed(500);
		link1.setNumberOfLanes(100);
		link1.setLength(500);
		
		// link without capacity restrictions
		link2.setAllowedModes(modes);
		link2.setCapacity(36000);
		link2.setFreespeed(10);
		link2.setNumberOfLanes(100);
		link2.setLength(500);
		
		// link without capacity restrictions
		link3.setAllowedModes(modes);
		link3.setCapacity(36000);
		link3.setFreespeed(500);
		link3.setNumberOfLanes(100);
		link3.setLength(500);
		
		// link without capacity restrictions
		link4.setAllowedModes(modes);
		link4.setCapacity(36000);
		link4.setFreespeed(10);
		link4.setNumberOfLanes(100);
		link4.setLength(500);

		// link with storage (3v ehicles per link) and flow (1 vehicle / 10 seconds) restrictions
		link5.setAllowedModes(modes);
		link5.setCapacity(360);
		link5.setFreespeed(2.5);
		link5.setNumberOfLanes(1);
		link5.setLength(37.5);

		// link without capacity restrictions
		link6.setAllowedModes(modes);
		link6.setCapacity(36000);
		link6.setFreespeed(500);
		link6.setNumberOfLanes(100);
		link6.setLength(500);
		
		network.addNode(node0);
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);
		network.addNode(node6);
	
		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
		network.addLink(link4);
		network.addLink(link5);
		network.addLink(link6);
	
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
