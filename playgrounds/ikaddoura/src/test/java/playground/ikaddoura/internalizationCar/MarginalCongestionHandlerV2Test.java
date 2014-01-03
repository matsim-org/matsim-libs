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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura
 *
 */
public class MarginalCongestionHandlerV2Test {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	private EventsManager events;
	private ScenarioImpl scenario;
	
	private Id testAgent1 = new IdImpl("testAgent1");
	private Id testAgent2 = new IdImpl("testAgent2");
	private Id testAgent3 = new IdImpl("testAgent3");
	
	private Id linkId1 = new IdImpl("link1");
	private Id linkId2 = new IdImpl("link2");
	private Id linkId3 = new IdImpl("link3");
	private Id linkId4 = new IdImpl("link4");
	
	// 2 agenten mit 5 sec Verzögerung
	@Test
	public final void testFlowCongestion(){
		
		loadScenario();
		setLinks_noStorageCapacityConstraints();
		setPopulation();
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
						
		MarginalCongestionHandlerImplV2 congestionHandler = new MarginalCongestionHandlerImplV2(this.events, this.scenario);

		// start agent 1...
		congestionHandler.handleEvent(new PersonDepartureEvent((double) 0, testAgent1, linkId1, "car"));
		congestionHandler.handleEvent(new LinkLeaveEvent((double) 1, testAgent1, linkId1, testAgent1));
		congestionHandler.handleEvent(new LinkEnterEvent((double) 1, testAgent1, linkId2, testAgent1));
		// start agent 2...
		congestionHandler.handleEvent(new PersonDepartureEvent((double) 5, testAgent2, linkId1, "car"));
		congestionHandler.handleEvent(new LinkLeaveEvent((double) 6, testAgent2, linkId1, testAgent2));
		congestionHandler.handleEvent(new LinkEnterEvent((double) 6, testAgent2, linkId2, testAgent2));
		
		// agent 1 kann ohne Probleme link 2 verlassen...
		congestionHandler.handleEvent(new LinkLeaveEvent((double) 52, testAgent1, linkId2, testAgent1));
		congestionHandler.handleEvent(new LinkEnterEvent((double) 52, testAgent1, linkId3, testAgent1));
		
		// agent 2 muss allerdings durch die flow capacity warten.
		congestionHandler.handleEvent(new LinkLeaveEvent((double) (52 + 10), testAgent2, linkId2, testAgent2));
		congestionHandler.handleEvent(new LinkEnterEvent((double) (52 + 10), testAgent2, linkId3, testAgent2));
		
		// *****************
		
		Assert.assertEquals("number of congestion events", 1, congestionEvents.size());

		MarginalCongestionEvent congEvent = congestionEvents.get(0);
		Assert.assertEquals("external delay", 5, congEvent.getDelay(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("congested link", linkId2.toString(), congEvent.getLinkId().toString());
		Assert.assertEquals("causing Agent", testAgent1.toString(), congEvent.getCausingAgentId().toString());
		Assert.assertEquals("affected Agent", testAgent2.toString(), congEvent.getAffectedAgentId().toString());
	}
	
	// 2 agenten mit 10 sec Verzögerung
	@Test
	public final void testNoCongestion(){
		
		loadScenario();
		setLinks_noStorageCapacityConstraints();
		setPopulation();
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
						
		MarginalCongestionHandlerImplV2 congestionHandler = new MarginalCongestionHandlerImplV2(this.events, this.scenario);

		// start agent 1...
		congestionHandler.handleEvent(new PersonDepartureEvent((double) 0, testAgent1, linkId1, "car"));
		congestionHandler.handleEvent(new LinkLeaveEvent((double) 1, testAgent1, linkId1, testAgent1));
		congestionHandler.handleEvent(new LinkEnterEvent((double) 1, testAgent1, linkId2, testAgent1));
		// start agent 2...
		congestionHandler.handleEvent(new PersonDepartureEvent((double) 10, testAgent2, linkId1, "car"));
		congestionHandler.handleEvent(new LinkLeaveEvent((double) 11, testAgent2, linkId1, testAgent2));
		congestionHandler.handleEvent(new LinkEnterEvent((double) 11, testAgent2, linkId2, testAgent2));
		
		// agent 1 kann ohne Probleme link 2 verlassen...
		congestionHandler.handleEvent(new LinkLeaveEvent((double) 51, testAgent1, linkId2, testAgent1));
		congestionHandler.handleEvent(new LinkEnterEvent((double) 51, testAgent1, linkId3, testAgent1));
		
		// agent 2 muss allerdings durch die flow capacity warten.
		congestionHandler.handleEvent(new LinkLeaveEvent((double) (51 + 10), testAgent2, linkId2, testAgent2));
		congestionHandler.handleEvent(new LinkEnterEvent((double) (51 + 10), testAgent2, linkId3, testAgent2));
		
		// *****************
		
		Assert.assertEquals("number of congestion events", 0, congestionEvents.size());
	}
	
//	// 2 agenten mit 5 sec Verzögerung, active Storage Capacity
//	@Test
//	public final void testFlowStorageCongestion(){
//		
//		loadScenario();
//		setLinks_noStorageCapacityConstraints();
//		setPopulation();
//		final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
//							
//		events.addHandler( new MarginalCongestionEventHandler() {
//
//			@Override
//			public void reset(int iteration) {				
//			}
//
//			@Override
//			public void handleEvent(MarginalCongestionEvent event) {
//				congestionEvents.add(event);
//			}
//			
//		});
//						
//		MarginalCongestionHandler congestionHandler = new MarginalCongestionHandler(this.events, this.scenario);
//
//		// start agent 1...
//		congestionHandler.handleEvent(ef.createAgentDepartureEvent(0, testAgent1, linkId1, "car"));
//		congestionHandler.handleEvent(ef.createLinkLeaveEvent(1, testAgent1, linkId1, testAgent1));
//		congestionHandler.handleEvent(ef.createLinkEnterEvent(1, testAgent1, linkId2, testAgent1));
//		// start agent 2...
//		congestionHandler.handleEvent(ef.createAgentDepartureEvent(5, testAgent2, linkId1, "car"));
//		congestionHandler.handleEvent(ef.createLinkLeaveEvent(6, testAgent2, linkId1, testAgent2));
//		congestionHandler.handleEvent(ef.createLinkEnterEvent(6, testAgent2, linkId2, testAgent2));
//		
//		// agent 1 kann ohne Probleme link 2 verlassen...
//		congestionHandler.handleEvent(ef.createLinkLeaveEvent(52, testAgent1, linkId2, testAgent1));
//		congestionHandler.handleEvent(ef.createLinkEnterEvent(52, testAgent1, linkId3, testAgent1));
//		
//		// agent 2 muss allerdings durch die flow capacity warten (52 + 10) plus durch die storage capacity (35)
//		congestionHandler.handleEvent(ef.createLinkLeaveEvent(52 + 10 + 35, testAgent2, linkId2, testAgent2));
//		congestionHandler.handleEvent(ef.createLinkEnterEvent(52 + 10 + 35, testAgent2, linkId3, testAgent2));
//		
//		// *****************
//		
//		Assert.assertEquals("number of congestion events", 2, congestionEvents.size());
//
//		MarginalCongestionEvent congEvent1 = congestionEvents.get(0);
//		MarginalCongestionEvent congEvent2 = congestionEvents.get(1);
//		
//		double totalDelay = congEvent1.getDelay() + congEvent2.getDelay();
//		Assert.assertEquals("wrong delay.", 40., totalDelay, MatsimTestUtils.EPSILON);
//
//		Assert.assertEquals("congested link", linkId2.toString(), congEvent1.getLinkId().toString());
//		Assert.assertEquals("causing Agent", testAgent1.toString(), congEvent1.getCausingAgentId().toString());
//		Assert.assertEquals("affected Agent", testAgent2.toString(), congEvent1.getAffectedAgentId().toString());
//		
//		Assert.assertEquals("congested link", linkId2.toString(), congEvent2.getLinkId().toString());
//		Assert.assertEquals("causing Agent", testAgent1.toString(), congEvent2.getCausingAgentId().toString());
//		Assert.assertEquals("affected Agent", testAgent2.toString(), congEvent2.getAffectedAgentId().toString());
//	}
	
	// 2 agenten mit 5 sec Verzögerung, active Storage Capacity durch 2
	@Test
	public final void testFlowStorageCongestion2(){
		
		loadScenario();
		setLinks_noStorageCapacityConstraints();
		setPopulation();
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
						
		MarginalCongestionHandlerImplV2 congestionHandler = new MarginalCongestionHandlerImplV2(this.events, this.scenario);

		// agent 3 blockiert link3
		congestionHandler.handleEvent(new PersonDepartureEvent((double) 0, testAgent3, linkId2, "car"));
		congestionHandler.handleEvent(new LinkLeaveEvent((double) 1, testAgent3, linkId2, testAgent3));
		congestionHandler.handleEvent(new LinkEnterEvent((double) 1, testAgent3, linkId3, testAgent3));

		// start agent 1...
		congestionHandler.handleEvent(new PersonDepartureEvent((double) 0, testAgent1, linkId1, "car"));
		congestionHandler.handleEvent(new LinkLeaveEvent((double) 1, testAgent1, linkId1, testAgent1));
		congestionHandler.handleEvent(new LinkEnterEvent((double) 1, testAgent1, linkId2, testAgent1));
		// start agent 2...
		congestionHandler.handleEvent(new PersonDepartureEvent((double) 5, testAgent2, linkId1, "car"));
		congestionHandler.handleEvent(new LinkLeaveEvent((double) 6, testAgent2, linkId1, testAgent2));
		congestionHandler.handleEvent(new LinkEnterEvent((double) 6, testAgent2, linkId2, testAgent2));
		
		// agent 1 kann ohne Probleme link 2 verlassen...
		congestionHandler.handleEvent(new LinkLeaveEvent((double) 52, testAgent1, linkId2, testAgent1));
		congestionHandler.handleEvent(new LinkEnterEvent((double) 52, testAgent1, linkId3, testAgent1));
		
		// agent 2 muss allerdings durch die flow capacity warten (51 + 10) plus durch die storage capacity (35)
		congestionHandler.handleEvent(new LinkLeaveEvent((double) (52 + 10 + 35), testAgent2, linkId2, testAgent2));
		congestionHandler.handleEvent(new LinkEnterEvent((double) (52 + 10 + 35), testAgent2, linkId3, testAgent2));
		
		// *****************

		MarginalCongestionEvent congEvent1 = congestionEvents.get(0);

		double totalDelay = congEvent1.getDelay();

		Assert.assertEquals("wrong delay.", 40., totalDelay, MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("number of congestion events", 1, congestionEvents.size());

		Assert.assertEquals("congested link", linkId2.toString(), congEvent1.getLinkId().toString());
		Assert.assertEquals("causing Agent", testAgent1.toString(), congEvent1.getCausingAgentId().toString());
		Assert.assertEquals("affected Agent", testAgent2.toString(), congEvent1.getAffectedAgentId().toString());
		Assert.assertEquals("capacity constraint", "storageCapacity", congEvent1.getCapacityConstraint());
		
	}
		
	// 3 agenten
	@Test
	public final void testFlowCongestion3agents(){
		
		loadScenario();
		setLinks_noStorageCapacityConstraints();
		setPopulation();
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
						
		MarginalCongestionHandlerImplV2 congestionHandler = new MarginalCongestionHandlerImplV2(this.events, this.scenario);

		// start agent 1...
		congestionHandler.handleEvent(new PersonDepartureEvent((double) 0, testAgent1, linkId1, "car"));
		congestionHandler.handleEvent(new LinkLeaveEvent((double) 1, testAgent1, linkId1, testAgent1));
		congestionHandler.handleEvent(new LinkEnterEvent((double) 1, testAgent1, linkId2, testAgent1));
		// start agent 2...
		congestionHandler.handleEvent(new PersonDepartureEvent((double) 4, testAgent2, linkId1, "car"));
		congestionHandler.handleEvent(new LinkLeaveEvent((double) 5, testAgent2, linkId1, testAgent2));
		congestionHandler.handleEvent(new LinkEnterEvent((double) 5, testAgent2, linkId2, testAgent2));
		// start agent 3...
		congestionHandler.handleEvent(new PersonDepartureEvent((double) 8, testAgent3, linkId1, "car"));
		congestionHandler.handleEvent(new LinkLeaveEvent((double) 9, testAgent3, linkId1, testAgent3));
		congestionHandler.handleEvent(new LinkEnterEvent((double) 9, testAgent3, linkId2, testAgent3));
		
		// agent 1 kann ohne Probleme link 2 verlassen...
		congestionHandler.handleEvent(new LinkLeaveEvent((double) 52, testAgent1, linkId2, testAgent1));
		congestionHandler.handleEvent(new LinkEnterEvent((double) 52, testAgent1, linkId3, testAgent1));
		
		// agent 2 muss allerdings durch die flow capacity warten.
		congestionHandler.handleEvent(new LinkLeaveEvent((double) (52 + 10), testAgent2, linkId2, testAgent2));
		congestionHandler.handleEvent(new LinkEnterEvent((double) (52 + 10), testAgent2, linkId3, testAgent2));
		
		// auch agent 3 muss durch die flow capacity warten.
		congestionHandler.handleEvent(new LinkLeaveEvent((double) (52 + 20), testAgent3, linkId2, testAgent3));
		congestionHandler.handleEvent(new LinkEnterEvent((double) (52 + 20), testAgent3, linkId3, testAgent3));
		
		// *****************
		
		Assert.assertEquals("number of congestion events", 3, congestionEvents.size());
		
		MarginalCongestionEvent congEvent1 = congestionEvents.get(0);
		Assert.assertEquals("external delay", 6, congEvent1.getDelay(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("congested link", linkId2.toString(), congEvent1.getLinkId().toString());
		Assert.assertEquals("causing Agent", testAgent1.toString(), congEvent1.getCausingAgentId().toString());
		Assert.assertEquals("affected Agent", testAgent2.toString(), congEvent1.getAffectedAgentId().toString());
		Assert.assertEquals("capacity constraint", "flowCapacity", congEvent1.getCapacityConstraint());
		
		MarginalCongestionEvent congEvent2 = congestionEvents.get(1);
		Assert.assertEquals("external delay", 10, congEvent2.getDelay(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("congested link", linkId2.toString(), congEvent2.getLinkId().toString());
		Assert.assertEquals("causing Agent", testAgent2.toString(), congEvent2.getCausingAgentId().toString());
		Assert.assertEquals("affected Agent", testAgent3.toString(), congEvent2.getAffectedAgentId().toString());
		Assert.assertEquals("capacity constraint", "flowCapacity", congEvent2.getCapacityConstraint());
		
		MarginalCongestionEvent congEvent3 = congestionEvents.get(2);
		Assert.assertEquals("external delay", 2, congEvent3.getDelay(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("congested link", linkId2.toString(), congEvent3.getLinkId().toString());
		Assert.assertEquals("causing Agent", testAgent1.toString(), congEvent3.getCausingAgentId().toString());
		Assert.assertEquals("affected Agent", testAgent3.toString(), congEvent3.getAffectedAgentId().toString());
		Assert.assertEquals("capacity constraint", "flowCapacity", congEvent3.getCapacityConstraint());
		
	}	
			
	// *************************************************************************************************************************

		private void setPopulation() {
			PopulationFactoryImpl popFactory = new PopulationFactoryImpl(scenario);
		
			Person person1 = popFactory.createPerson(testAgent1);
			Plan plan1 = popFactory.createPlan();

			Person person2 = popFactory.createPerson(testAgent2);
			Plan plan2 = popFactory.createPlan();
			
			Person person3 = popFactory.createPerson(testAgent3);
			Plan plan3 = popFactory.createPlan();			
			
			Activity act = popFactory.createActivityFromLinkId("home", linkId1);
			act.setEndTime(0);
			Leg leg = popFactory.createLeg("car");
			LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
			List<Id> linkIds = new ArrayList<Id>();
			linkIds.add(linkId2);
			linkIds.add(linkId3);
			NetworkRoute route = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
			route.setLinkIds(linkId1, linkIds, linkId4);
			leg.setRoute(route);
			
			plan1.addActivity(act);
			plan1.addLeg(leg);
			plan2.addActivity(act);
			plan2.addLeg(leg);
			plan3.addActivity(act);
			plan3.addLeg(leg);
	
			person1.addPlan(plan1);
			person2.addPlan(plan2);
			person3.addPlan(plan3);
			
			Population population = scenario.getPopulation();
			population.addPerson(person1);
			population.addPerson(person2);
			population.addPerson(person3);
		}

	private void loadScenario() {
	
		this.scenario = null;
		this.events = null;
		
		// (0)-----link1-----(1)-----link2-----(2)-----link3-----(3)-----link4-----(4)
		
		Config config = ConfigUtils.createConfig();
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		this.scenario = (ScenarioImpl)(ScenarioUtils.createScenario(config));
	
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
	}

	private void setLinks_noStorageCapacityConstraints(){
		Set<String> modes = new HashSet<String>();
		modes.add("car");
		
		Link link1 = this.scenario.getNetwork().getLinks().get(linkId1);
		link1.setAllowedModes(modes);
		link1.setCapacity(7200); // 7200 --> 2 cars/sec 
		link1.setFreespeed(10); // 10 --> 36 km/h // 50 sec pro 500 m
		link1.setNumberOfLanes(1);
		link1.setLength(500);
		
		Link link2 = this.scenario.getNetwork().getLinks().get(linkId2);
		link2.setAllowedModes(modes);
		link2.setCapacity(360); // 360 --> every 10 sec one car
		link2.setFreespeed(10); // 10 --> 36 km/h // 50 sec pro 500 m
		link2.setNumberOfLanes(1);
		link2.setLength(500);
		
		Link link3 = this.scenario.getNetwork().getLinks().get(linkId3);
		link3.setAllowedModes(modes);
		link3.setCapacity(7200); // 7200 --> 2 cars/sec 
		link3.setFreespeed(10); // 10 --> 36 km/h // 50 sec pro 500 m
		link3.setNumberOfLanes(1);
		link3.setLength(15);
		
		Link link4 = this.scenario.getNetwork().getLinks().get(linkId4);
		link4.setAllowedModes(modes);
		link4.setCapacity(7200); // 7200 --> 2 cars/sec 
		link4.setFreespeed(10); // 10 --> 36 km/h // 50 sec pro 500 m
		link4.setNumberOfLanes(1);
		link4.setLength(500);
	}
	
}
