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
package playground.vsp.congestion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.handlers.CongestionEventHandler;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV7;
import playground.vsp.congestion.handlers.CongestionHandlerImplV8;

/**
 * Simple scenario setup:
 * Three agents moving along a corridor with unlimited storage capacity; arriving simultaneously at the bottleneck.
 *
 * @author ikaddoura , lkroeger
 *
 */

public class MarginalCongestionHandlerFlowQueueQsimTest {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	private EventsManager events;

	private Id<Person> testAgent1 = Id.create("agentA", Person.class);
	private Id<Person> testAgent2 = Id.create("agentB", Person.class);
	private Id<Person> testAgent3 = Id.create("agentC", Person.class);

	private Id<Link> linkId1 = Id.create("link1", Link.class);
	private Id<Link> linkId2 = Id.create("link2", Link.class);
	private Id<Link> linkId3 = Id.create("link3", Link.class);
	private Id<Link> linkId4 = Id.create("link4", Link.class);
	private Id<Link> linkId5 = Id.create("link5", Link.class);

	double avgValue1 = 0.0;
	double avgValue2 = 0.0;
	double avgOldValue1 = 0.0;
	double avgOldValue2 = 0.0;
	double avgValue3 = 0.0;
	double avgValue4 = 0.0;
	double avgOldValue3 = 0.0;
	double avgOldValue4 = 0.0;

	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	/**
	 * V3
	 */
	@Test
	final void testFlowCongestion_3agents_V3(){

		Scenario sc = loadScenario1();
		setPopulation1(sc);

		final List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();
		final CongestionHandlerImplV3 congestionHandler = new CongestionHandlerImplV3(events, sc);

		events.addHandler( new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}
		});

		events.addHandler(congestionHandler);

		PrepareForSimUtils.createDefaultPrepareForSim(sc).run();
		QSim sim = createQSim(sc, events);
		sim.run();

		for (CongestionEvent event : congestionEvents) {
			Assertions.assertEquals(3., event.getDelay(), MatsimTestUtils.EPSILON, "here the delay should be equal to the inverse of the flow capacity");
		}

		Assertions.assertEquals(9., congestionHandler.getTotalDelay(), MatsimTestUtils.EPSILON, "wrong total delay");
		Assertions.assertEquals(9., congestionHandler.getTotalInternalizedDelay(), MatsimTestUtils.EPSILON, "wrong total internalized delay");

	}

	/**
	 * V7
	 */
	@Test
	final void testFlowCongestion_3agents_V7(){

		Scenario sc = loadScenario1();
		setPopulation1(sc);

		final List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();
		final CongestionHandlerImplV7 congestionHandler = new CongestionHandlerImplV7(events, sc);

		events.addHandler( new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}
		});

		events.addHandler(congestionHandler);

		PrepareForSimUtils.createDefaultPrepareForSim(sc).run();
		QSim sim = createQSim(sc, events);
		sim.run();

		for (CongestionEvent event : congestionEvents) {
			if (event.getCausingAgentId().toString().equals("agentC") && event.getAffectedAgentId().toString().equals("agentB")) {
				Assertions.assertEquals(3., event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay");
			}

			if (event.getCausingAgentId().toString().equals("agentC") && event.getAffectedAgentId().toString().equals("agentA")) {
				Assertions.assertEquals(6., event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay");
			}

			if (event.getCausingAgentId().toString().equals("agentB") && event.getAffectedAgentId().toString().equals("agentA")) {
				Assertions.assertEquals(6., event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay");
			}
		}

		Assertions.assertEquals(9., congestionHandler.getTotalDelay(), MatsimTestUtils.EPSILON, "wrong total delay");

		// the second agent is 3 sec delayed and charges the first agent with these 3 sec
		// the third agent is 6 sec delayed and charges the first and the second agent with each 6 sec
		Assertions.assertEquals(15., congestionHandler.getTotalInternalizedDelay(), MatsimTestUtils.EPSILON, "wrong total internalized delay");
	}

	/**
	 * V8
	 */
	@Test
	final void testFlowCongestion_3agents_V8(){

		Scenario sc = loadScenario1();
		setPopulation1(sc);

		final List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();
		final CongestionHandlerImplV8 congestionHandler = new CongestionHandlerImplV8(events, sc);

		events.addHandler( new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}
		});

		events.addHandler(congestionHandler);

		PrepareForSimUtils.createDefaultPrepareForSim(sc).run();
		QSim sim = createQSim(sc, events);
		sim.run();

		for (CongestionEvent event : congestionEvents) {
			if (event.getCausingAgentId().toString().equals("agentC") && event.getAffectedAgentId().toString().equals("agentB")) {
				Assertions.assertEquals(3., event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay");
			}

			if (event.getCausingAgentId().toString().equals("agentC") && event.getAffectedAgentId().toString().equals("agentA")) {
				Assertions.assertEquals(3., event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay");
			}

			if (event.getCausingAgentId().toString().equals("agentB") && event.getAffectedAgentId().toString().equals("agentA")) {
				Assertions.assertEquals(3., event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay");
			}
		}

		Assertions.assertEquals(9., congestionHandler.getTotalDelay(), MatsimTestUtils.EPSILON, "wrong total delay");

		// the second agent is 3 sec delayed and charges the first agent with these 3 sec
		// the third agent is 6 sec delayed and charges the first and the second agent with each 6 sec
		Assertions.assertEquals(9., congestionHandler.getTotalInternalizedDelay(), MatsimTestUtils.EPSILON, "wrong total internalized delay");
	}

	private void setPopulation1(Scenario scenario) {

	Population population = scenario.getPopulation();
	PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
	LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

	Activity workActLink5 = popFactory.createActivityFromLinkId("work", linkId5);

	// leg: 1,2,3,4,5
	Leg leg_1_5 = popFactory.createLeg("car");
	List<Id<Link>> linkIds234 = new ArrayList<Id<Link>>();
	linkIds234.add(linkId2);
	linkIds234.add(linkId3);
	linkIds234.add(linkId4);
	NetworkRoute route1_5 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
	route1_5.setLinkIds(linkId1, linkIds234, linkId5);
	leg_1_5.setRoute(route1_5);

	Person person1 = popFactory.createPerson(testAgent1);
	Plan plan1 = popFactory.createPlan();
	Activity homeActLink1_1 = popFactory.createActivityFromLinkId("home", linkId1);
	homeActLink1_1.setEndTime(100);
	plan1.addActivity(homeActLink1_1);
	plan1.addLeg(leg_1_5);
	plan1.addActivity(workActLink5);
	person1.addPlan(plan1);
	population.addPerson(person1);

	Person person2 = popFactory.createPerson(testAgent2);
	Plan plan2 = popFactory.createPlan();
	Activity homeActLink1_2 = popFactory.createActivityFromLinkId("home", linkId1);
	homeActLink1_2.setEndTime(100);
	plan2.addActivity(homeActLink1_2);
	{
		Leg leg = popFactory.createLeg(leg_1_5.getMode());
		PopulationUtils.copyFromTo(leg_1_5, leg);
		plan2.addLeg(leg);
	}
	plan2.addActivity(workActLink5);
	person2.addPlan(plan2);
	population.addPerson(person2);

	Person person3 = popFactory.createPerson(testAgent3);
	Plan plan3 = popFactory.createPlan();
	Activity homeActLink1_3 = popFactory.createActivityFromLinkId("home", linkId1);
	homeActLink1_3.setEndTime(100);
	plan3.addActivity(homeActLink1_3);
	{
		Leg leg = popFactory.createLeg(leg_1_5.getMode());
		PopulationUtils.copyFromTo(leg_1_5, leg);
		plan3.addLeg(leg);
	}
	plan3.addActivity(workActLink5);
	person3.addPlan(plan3);
	population.addPerson(person3);
}

	private Scenario loadScenario1() {

		// (0)				(1)				(2)				(3)				(4)				(5)
		//    -----link1----   ----link2----   ----link3----   ----link4----   ----link5----

		Config config = testUtils.createConfig();
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		qSimConfigGroup.setRemoveStuckVehicles(true);
		qSimConfigGroup.setStuckTime(3600.0);
		Scenario scenario = (ScenarioUtils.createScenario(config));

		Network network = (Network) scenario.getNetwork();
		network.setEffectiveCellSize(7.5);
		network.setCapacityPeriod(3600.);

		Node node0 = network.getFactory().createNode(Id.create("0", Node.class), new Coord(0., 0.));
		Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord(100., 0.));
		Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord(200., 0.));
		Node node3 = network.getFactory().createNode(Id.create("3", Node.class), new Coord(300., 0.));
		Node node4 = network.getFactory().createNode(Id.create("4", Node.class), new Coord(400., 0.));
		Node node5 = network.getFactory().createNode(Id.create("5", Node.class), new Coord(500., 0.));

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

		// capacity: one car every 3 sec
		link3.setAllowedModes(modes);
		link3.setCapacity(1200);
		link3.setFreespeed(250); // one time step
		link3.setNumberOfLanes(100);
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
		return new QSimBuilder(sc.getConfig()).useDefaults().build(sc, events);
	}

}
