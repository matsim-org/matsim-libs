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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
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
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
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

public class MarginalCongestionHandlerV3QsimTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	private EventsManager events;
	
	private Id testAgent1 = new IdImpl("testAgent1");
	private Id testAgent2 = new IdImpl("testAgent2");
	private Id testAgent3 = new IdImpl("testAgent3");
	
	private Id linkId1 = new IdImpl("link1");
	private Id linkId2 = new IdImpl("link2");
	private Id linkId3 = new IdImpl("link3");
	private Id linkId4 = new IdImpl("link4");
	private Id linkId5 = new IdImpl("link5");
	
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	
	// the flow capacity on link 3 (1car / 10 seconds) is activated by the first agent,
	// then the storage capacity on link 3 (only one car) is reached, too
	// finally, one car on the link before is delayed
	@Test
	public final void testFlowAndStorageCongestion_3agents(){
		
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
		
		events.addHandler(new MarginalCongestionHandlerV3(events, (ScenarioImpl) sc));
				
		QSim sim = createQSim(sc, events);
		sim.run();
						
		for (MarginalCongestionEvent event : congestionEvents) {
		
			System.out.println(event.toString());
			
			if (event.getCausingAgentId().toString().equals(this.testAgent1.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent2.toString())) {				
				Assert.assertEquals("wrong delay.", 10.0, event.getDelay(), MatsimTestUtils.EPSILON);
			} else if ((event.getCausingAgentId().toString().equals(this.testAgent2.toString())) && (event.getAffectedAgentId().toString().equals(this.testAgent3.toString())) && (event.getTime() == 116.0)) {
				Assert.assertEquals("wrong delay.", 10.0, event.getDelay(), MatsimTestUtils.EPSILON);
			} else if ((event.getCausingAgentId().toString().equals(this.testAgent1.toString())) && (event.getAffectedAgentId().toString().equals(this.testAgent3.toString())) && (event.getTime() == 126.0)) {
				Assert.assertEquals("wrong delay.", 9.0, event.getDelay(), MatsimTestUtils.EPSILON);
			}
		}
		
	}
	
	// ################################################################################################################################
		
	private void setPopulation1(Scenario scenario) {
		
		Population population = scenario.getPopulation();
		PopulationFactoryImpl popFactory = new PopulationFactoryImpl(scenario);
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity workActLink5 = popFactory.createActivityFromLinkId("work", linkId5);
		
		// leg: 1,2,3,4,5
		Leg leg_1_5 = popFactory.createLeg("car");
		List<Id> linkIds234 = new ArrayList<Id>();
		linkIds234.add(linkId2);
		linkIds234.add(linkId3);
		linkIds234.add(linkId4);
		NetworkRoute route1_5 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
		route1_5.setLinkIds(linkId1, linkIds234, linkId5);
		leg_1_5.setRoute(route1_5);
		
		// ################################################################
		// first agent activating the flow capacity on link3
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity homeActLink1_1 = popFactory.createActivityFromLinkId("home", linkId1);
		homeActLink1_1.setEndTime(100);
		plan1.addActivity(homeActLink1_1);
		plan1.addLeg(leg_1_5);
		plan1.addActivity(workActLink5);
		person1.addPlan(plan1);
		population.addPerson(person1);
		
		// ################################################################
		// second agent delayed on link3; blocking link3
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity homeActLink1_2 = popFactory.createActivityFromLinkId("home", linkId1);
		homeActLink1_2.setEndTime(101);
		plan2.addActivity(homeActLink1_2);
		plan2.addLeg(leg_1_5);
		plan2.addActivity(workActLink5);
		person2.addPlan(plan2);
		population.addPerson(person2);			
		
		// ################################################################
		// third agent delayed on link2 (spill-back)
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity homeActLink1_3 = popFactory.createActivityFromLinkId("home", linkId1);
		homeActLink1_3.setEndTime(102);
		plan3.addActivity(homeActLink1_3);
		plan3.addLeg(leg_1_5);
		plan3.addActivity(workActLink5);
		person3.addPlan(plan3);
		population.addPerson(person3);

	}
	
	private Scenario loadScenario1() {
			
		// (0)				(1)				(2)				(3)				(4)				(5)
		//    -----link1----   ----link2----   ----link3----   ----link4----   ----link5----   
		
		Config config = testUtils.loadConfig(null);
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		qSimConfigGroup.setRemoveStuckVehicles(true);
		qSimConfigGroup.setStuckTime(3600.0);
		Scenario scenario = (ScenarioImpl)(ScenarioUtils.createScenario(config));
	
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setEffectiveCellSize(7.5);
		network.setCapacityPeriod(3600.);
		
		Node node0 = network.getFactory().createNode(new IdImpl("0"), scenario.createCoord(0., 0.));
		Node node1 = network.getFactory().createNode(new IdImpl("1"), scenario.createCoord(100., 0.));
		Node node2 = network.getFactory().createNode(new IdImpl("2"), scenario.createCoord(200., 0.));
		Node node3 = network.getFactory().createNode(new IdImpl("3"), scenario.createCoord(300., 0.));
		Node node4 = network.getFactory().createNode(new IdImpl("4"), scenario.createCoord(400., 0.));
		Node node5 = network.getFactory().createNode(new IdImpl("5"), scenario.createCoord(500., 0.));
		
		Link link1 = network.getFactory().createLink(this.linkId1, node0, node1);
		Link link2 = network.getFactory().createLink(this.linkId2, node1, node2);
		Link link3 = network.getFactory().createLink(this.linkId3, node2, node3);
		Link link4 = network.getFactory().createLink(this.linkId4, node3, node4);
		Link link5 = network.getFactory().createLink(this.linkId5, node4, node5);

		Set<String> modes = new HashSet<String>();
		modes.add("car");
		
		// link without capacity restrictions
		link1.setAllowedModes(modes);
		link1.setCapacity(999999);
		link1.setFreespeed(250); // one time step
		link1.setNumberOfLanes(100);
		link1.setLength(500);
		
		// link without capacity restrictions
		link2.setAllowedModes(modes);
		link2.setCapacity(999999);
		link2.setFreespeed(166.66666667); // two time steps
		link2.setNumberOfLanes(100);
		link2.setLength(500);
		
		// link meant to reach storage capacity: space for one car, flow capacity: one car every 10 sec
		link3.setAllowedModes(modes);
		link3.setCapacity(360);
		link3.setFreespeed(250); // one time step
		link3.setNumberOfLanes(1);
		link3.setLength(7.5);
		
		// link without capacity restrictions
		link4.setAllowedModes(modes);
		link4.setCapacity(999999);
		link4.setFreespeed(166.66666667); // two time steps
		link4.setNumberOfLanes(100);
		link4.setLength(500);
		
		// link without capacity restrictions
		link5.setAllowedModes(modes);
		link5.setCapacity(999999);
		link5.setFreespeed(250); // one time step
		link5.setNumberOfLanes(100);
		link5.setLength(500);
		
		network.addNode(node0);
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);

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
		DefaultQSimEngineFactory netsimEngFactory = new DefaultQSimEngineFactory();
		QNetsimEngine netsimEngine = netsimEngFactory.createQSimEngine(qSim1);
		qSim1.addMobsimEngine(netsimEngine);
		qSim1.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim1.addMobsimEngine(teleportationEngine);
		QSim qSim = qSim1;
		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}
	
}
