/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LaneLeaveEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesFactory;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.testcases.MatsimTestUtils;

/**
 * test flow capacity of links with lanes in the simulation, i.e. the number of vehicles that leave the link or lane per hour
 *
 * @author tthunig
 *
 */
public class SimulatedLaneFlowCapacityTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * create this network:
	 * 0 ----- 1 ----- 2 ----- 3
	 *
	 */
	private static void initNetwork(Network network) {
		Node node0 = network.getFactory().createNode(Id.create("0", Node.class), new Coord(0, 0));
		Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord(1, 0));
		Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord(2, 0));
		Node node3 = network.getFactory().createNode(Id.create("3", Node.class), new Coord(3, 0));
		network.addNode(node0);
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		Link l0 = network.getFactory().createLink(Id.createLinkId(0), node0, node1);
		l0.setLength(25000.0);
		l0.setCapacity(4000.0);
		network.addLink(l0);
		Link l1 = network.getFactory().createLink(Id.createLinkId(1), node1, node2);
		l1.setLength(25000.0);
		l1.setFreespeed(250.0);
		l1.setCapacity(1800.0);
		l1.setNumberOfLanes(2.0);
		network.addLink(l1);
		Link l2 = network.getFactory().createLink(Id.createLinkId(2), node2, node3);
		l2.setLength(25000.0);
		l2.setFreespeed(250.0);
		network.addLink(l2);
	}

	/**
	 * create a lane on link 1 representing the given number of lanes.
	 * the lanes capacity is 900 times the number of represented lanes.
	 *
	 */
	private static void createOneLane(Scenario scenario, int numberOfRepresentedLanes) {
		scenario.getConfig().qsim().setUseLanes(true);
		Lanes lanes = scenario.getLanes();
		LanesFactory builder = lanes.getFactory();
		//lanes for link 1
		LanesToLinkAssignment lanesForLink1 = builder.createLanesToLinkAssignment(Id.create(1, Link.class));
		Lane link1FirstLane = builder.createLane(Id.create("1.ol", Lane.class));
		link1FirstLane.addToLaneId(Id.create(1, Lane.class));
		link1FirstLane.setNumberOfRepresentedLanes(2.0);
		link1FirstLane.setStartsAtMeterFromLinkEnd(25000.0);
		link1FirstLane.setCapacityVehiclesPerHour(1800.0);
		lanesForLink1.addLane(link1FirstLane);

		Lane link1lane1 = builder.createLane(Id.create(1, Lane.class));
		link1lane1.addToLinkId(Id.createLinkId(2));
		link1lane1.setStartsAtMeterFromLinkEnd(10000.0);
		link1lane1.setNumberOfRepresentedLanes(numberOfRepresentedLanes);
		link1lane1.setCapacityVehiclesPerHour(numberOfRepresentedLanes * 900.0);
		lanesForLink1.addLane(link1lane1);
		lanes.addLanesToLinkAssignment(lanesForLink1);
	}

	/**
	 * create three lanes on link 1.
	 * the first and the third one represent 1 lane each, the second one represents 2 lanes.
	 * lane 1 and 3 have a capacity of 900 each; lane 2 has a capacity of 2000, which is higher than the link capacity itself.
	 *
	 */
	private static void createThreeLanes(Scenario scenario) {
		scenario.getConfig().qsim().setUseLanes(true);
		Lanes lanes = scenario.getLanes();
		LanesFactory builder = lanes.getFactory();
		//lanes for link 1
		LanesToLinkAssignment lanesForLink1 = builder.createLanesToLinkAssignment(Id.create("1", Link.class));

		Lane link1FirstLane = builder.createLane(Id.create("1.ol", Lane.class));
		link1FirstLane.addToLaneId(Id.create("1", Lane.class));
		link1FirstLane.addToLaneId(Id.create("2", Lane.class));
		link1FirstLane.addToLaneId(Id.create("3", Lane.class));
		link1FirstLane.setNumberOfRepresentedLanes(2.0);
		link1FirstLane.setStartsAtMeterFromLinkEnd(25000.0);
		link1FirstLane.setCapacityVehiclesPerHour(4000.0);
		lanesForLink1.addLane(link1FirstLane);

		Lane link1lane1 = builder.createLane(Id.create(1, Lane.class));
		link1lane1.addToLinkId(Id.createLinkId(2));
		link1lane1.setStartsAtMeterFromLinkEnd(10000.0);
		link1lane1.setCapacityVehiclesPerHour(900.0);
		lanesForLink1.addLane(link1lane1);

		Lane link1lane2 = builder.createLane(Id.create(2, Lane.class));
		link1lane2.addToLinkId(Id.createLinkId(2));
		link1lane2.setNumberOfRepresentedLanes(2);
		link1lane2.setStartsAtMeterFromLinkEnd(10000.0);
		link1lane2.setCapacityVehiclesPerHour(2000.0);
		lanesForLink1.addLane(link1lane2);

		Lane link1lane3 = builder.createLane(Id.create(3, Lane.class));
		link1lane3.addToLinkId(Id.createLinkId(2));
		link1lane3.setStartsAtMeterFromLinkEnd(10000.0);
		link1lane3.setCapacityVehiclesPerHour(900.0);
		lanesForLink1.addLane(link1lane3);

		lanes.addLanesToLinkAssignment(lanesForLink1);
	}

	/**
	 * create a population of 5000 persons traveling from link 0 to link 2.
	 * they all start 50 minutes after midnight and then queue on link 0 which lets one of them depart every second.
	 * for analysis we then use flow values from the second hour.
	 *
	 */
	private static void initPopulation(Population population) {
		// create enough persons to be able to check simulated capacity of link 1
		for (int i = 0; i < 5000; i++) {
			Person person = population.getFactory().createPerson(Id.createPersonId(i));
			// create a start activity at link 0 with end time 50*60
			Activity startAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId("0"));
			startAct.setEndTime(50*60);
			// create a drain activity at link 2
			Activity drainAct = population.getFactory().createActivityFromLinkId(
					"dummy", Id.createLinkId("2"));
			// create a dummy leg
			Leg leg = population.getFactory().createLeg(TransportMode.car);
			leg.setRoute(RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId("0"), Collections.singletonList(Id.createLinkId("1")), Id.createLinkId("2")));
			// create a plan for the person that contains all this information
			Plan plan = population.getFactory().createPlan();
			plan.addActivity(startAct);
			plan.addLeg(leg);
			plan.addActivity(drainAct);
			// store information in population
			person.addPlan(plan);
			population.addPerson(person);
		}
	}

	/**
	 * test simulated capacity of link 1 in case without lanes.
	 * the capacity should correspond to the given flow capacity of the link
	 */
	@Test
	void testCapacityWoLanes() {
		Config config = ConfigUtils.createConfig();
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		ActivityParams dummyAct = new ActivityParams("dummy");
		dummyAct.setTypicalDuration(12 * 3600);
		config.scoring().addActivityParams(dummyAct);

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		initNetwork(scenario.getNetwork());
		initPopulation(scenario.getPopulation());

		EventsManager events = EventsUtils.createEventsManager();
		SimulatedCapacityHandler simulatedCapacity = new SimulatedCapacityHandler();
		events.addHandler(simulatedCapacity);
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		new QSimBuilder(scenario.getConfig()) //
			.useDefaults() //
			.build(scenario, events) //
			.run();

		// check simulated capacity values
		assertEquals(1800, simulatedCapacity.getSimulatedLinkCapacity(), MatsimTestUtils.EPSILON);
	}

	/**
	 * test simulated capacities of link 1 in case of one lane representing one lane.
	 * the capacity of the link should correspond to the capacity of the lane, also when it is less than the link capacity given in the network.
	 */
	@Test
	void testCapacityWithOneLaneOneLane() {
		Config config = ConfigUtils.createConfig();
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		ActivityParams dummyAct = new ActivityParams("dummy");
		dummyAct.setTypicalDuration(12 * 3600);
		config.scoring().addActivityParams(dummyAct);

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		initNetwork(scenario.getNetwork());
		createOneLane(scenario, 1);
		initPopulation(scenario.getPopulation());

		EventsManager events = EventsUtils.createEventsManager();
		SimulatedCapacityHandler simulatedCapacity = new SimulatedCapacityHandler();
		events.addHandler(simulatedCapacity);

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		new QSimBuilder(scenario.getConfig()) //
			.useDefaults() //
			.build(scenario, events) //
			.run();

		// check simulated capacity values
		assertEquals(simulatedCapacity.getSimulatedLaneCapacity(Id.create("1", Lane.class)), simulatedCapacity.getSimulatedLinkCapacity(), MatsimTestUtils.EPSILON);
		assertEquals(1800, simulatedCapacity.getSimulatedLaneCapacity(Id.create("1.ol", Lane.class)), MatsimTestUtils.EPSILON);
		assertEquals(900, simulatedCapacity.getSimulatedLaneCapacity(Id.create("1", Lane.class)), MatsimTestUtils.EPSILON);
	}

	/**
	 * test simulated capacities of link 1 in case of one lane representing two lanes.
	 * the capacity of the link should correspond to the capacity of the lane, also when it is less than the link capacity given in the network.
	 */
	@Test
	void testCapacityWithOneLaneTwoLanes() {
		Config config = ConfigUtils.createConfig();
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		ActivityParams dummyAct = new ActivityParams("dummy");
		dummyAct.setTypicalDuration(12 * 3600);
		config.scoring().addActivityParams(dummyAct);

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		initNetwork(scenario.getNetwork());
		createOneLane(scenario, 2);
		initPopulation(scenario.getPopulation());

		EventsManager events = EventsUtils.createEventsManager();
		SimulatedCapacityHandler simulatedCapacity = new SimulatedCapacityHandler();
		events.addHandler(simulatedCapacity);
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		new QSimBuilder(scenario.getConfig()) //
			.useDefaults() //
			.build(scenario, events) //
			.run();

		// check simulated capacity values
		assertEquals(simulatedCapacity.getSimulatedLaneCapacity(Id.create("1", Lane.class)), simulatedCapacity.getSimulatedLinkCapacity(), MatsimTestUtils.EPSILON);
		assertEquals(1800, simulatedCapacity.getSimulatedLaneCapacity(Id.create("1.ol", Lane.class)), MatsimTestUtils.EPSILON);
		assertEquals(2*900, simulatedCapacity.getSimulatedLaneCapacity(Id.create("1", Lane.class)), MatsimTestUtils.EPSILON);
	}

	/**
	 * test simulated capacities of link 1 in case of three lanes.
	 * the simulated capacity of the link should correspond to the sum of capacities of all three lanes, also when it is less than the link capacity given in the network.
	 * Interestingly, it also corresponds to this sum, if it is more than the link capacity given in the network.
	 * And, finally, it still only uses the lane capacity given in the network, when it is higher than the link capacity (see lane 2 here).
	 */
	@Test
	void testCapacityWithThreeLanes() {
		Config config = ConfigUtils.createConfig();
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		ActivityParams dummyAct = new ActivityParams("dummy");
		dummyAct.setTypicalDuration(12 * 3600);
		config.scoring().addActivityParams(dummyAct);

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		initNetwork(scenario.getNetwork());
		createThreeLanes(scenario);
		initPopulation(scenario.getPopulation());

		EventsManager events = EventsUtils.createEventsManager();
		SimulatedCapacityHandler simulatedCapacity = new SimulatedCapacityHandler();
		events.addHandler(simulatedCapacity);
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		new QSimBuilder(scenario.getConfig()) //
			.useDefaults() //
			.build(scenario, events) //
			.run();

		// check simulated capacity values
		double lane1Cap = simulatedCapacity.getSimulatedLaneCapacity(Id.create("1", Lane.class));
		double lane2Cap = simulatedCapacity.getSimulatedLaneCapacity(Id.create("2", Lane.class));
		double lane3Cap = simulatedCapacity.getSimulatedLaneCapacity(Id.create("3", Lane.class));
		double originalLaneCap = simulatedCapacity.getSimulatedLaneCapacity(Id.create("1.ol", Lane.class));
		System.out.println("LeaveEvents: " + originalLaneCap + ", " + lane1Cap + ", " + lane2Cap + ", " + lane3Cap);
		assertEquals(lane1Cap + lane2Cap + lane3Cap, simulatedCapacity.getSimulatedLinkCapacity(), MatsimTestUtils.EPSILON);
		assertEquals(4000, originalLaneCap, MatsimTestUtils.EPSILON);
		assertEquals(900, lane1Cap, MatsimTestUtils.EPSILON);
		assertEquals(2000, lane2Cap, MatsimTestUtils.EPSILON);
		assertEquals(900, lane3Cap, MatsimTestUtils.EPSILON);
	}


	/**
	 * event handler that counts vehicles that leave link 1 and lanes on link 1.
	 *
	 * @author tthunig
	 */
	static class SimulatedCapacityHandler implements LinkLeaveEventHandler, LaneLeaveEventHandler{

		private double linkCapacity;
		private Map<Id<Lane>, Double> laneCapacities = new HashMap<>();

		@Override
		public void reset(int iteration) {
			linkCapacity = 0;
			laneCapacities = new HashMap<>();
		}

		@Override
		public void handleEvent(LaneLeaveEvent event) {
			// count lane leave events on link 1 in the second hour
			if (event.getLinkId().equals(Id.createLinkId(1)) && event.getTime() >= 3600 && event.getTime() < 2*3600){
				if (!laneCapacities.containsKey(event.getLaneId())){
					laneCapacities.put(event.getLaneId(), 0.);
				}
				laneCapacities.put(event.getLaneId(), laneCapacities.get(event.getLaneId())+1);
			}
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			// count link leave events on link 1 in the second hour
			if (event.getLinkId().equals(Id.createLinkId(1)) && event.getTime() >= 3600 && event.getTime() < 2*3600){
				linkCapacity++;
			}

		}

		double getSimulatedLinkCapacity(){
			return linkCapacity;
		}

		double getSimulatedLaneCapacity(Id<Lane> laneId){
			return laneCapacities.containsKey(laneId)? laneCapacities.get(laneId) : 0;
		}
	}
}
