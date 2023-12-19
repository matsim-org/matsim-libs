/* *********************************************************************** *
 * project: org.matsim.*
 * QueueLaneTest
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
package org.matsim.core.mobsim.qsim.qnetsimengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.interfaces.NetsimNetwork;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesFactory;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Test for QLinkLanes' and QLanes capacity calculations
 *
 * @author dgrether
 */
public class QLinkLanesTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


  private static void initNetwork(Network network) {
		Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord((double) 1, (double) 0));
		Node node3 = network.getFactory().createNode(Id.create("3", Node.class), new Coord((double) 2, (double) 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		Link l1 = network.getFactory().createLink(Id.create("1", Link.class), node1, node2);
		l1.setLength(1005.0);
		l1.setFreespeed(15.0);
		l1.setCapacity(1800.0);
		l1.setNumberOfLanes(2.0);
		network.addLink(l1);
		Link l2 = network.getFactory().createLink(Id.create("2", Link.class), node2, node3);
		network.addLink(l2);
  }

	private static void createOneLane(Scenario scenario, int numberOfRepresentedLanes) {
		scenario.getConfig().qsim().setUseLanes(true);
		Lanes lanes = scenario.getLanes();
		LanesFactory builder = lanes.getFactory();
		//lanes for link 1
		LanesToLinkAssignment lanesForLink1 = builder.createLanesToLinkAssignment(Id.create(1, Link.class));
		Lane link1FirstLane = builder.createLane(Id.create("1.ol", Lane.class));
		link1FirstLane.addToLaneId(Id.create(1, Lane.class));
		link1FirstLane.setNumberOfRepresentedLanes(2.0);
		link1FirstLane.setStartsAtMeterFromLinkEnd(1005.0);
		link1FirstLane.setCapacityVehiclesPerHour(1800.0);
		lanesForLink1.addLane(link1FirstLane);

		Lane link1lane1 = builder.createLane(Id.create(1, Lane.class));
		link1lane1.addToLinkId(Id.create(2, Link.class));
		link1lane1.setStartsAtMeterFromLinkEnd(105.0);
		link1lane1.setNumberOfRepresentedLanes(numberOfRepresentedLanes);
		link1lane1.setCapacityVehiclesPerHour(numberOfRepresentedLanes * 900.0);
		lanesForLink1.addLane(link1lane1);
		lanes.addLanesToLinkAssignment(lanesForLink1);
	}

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
		link1FirstLane.setStartsAtMeterFromLinkEnd(1005.0);
		link1FirstLane.setCapacityVehiclesPerHour(1800.0);
		lanesForLink1.addLane(link1FirstLane);

		Lane link1lane1 = builder.createLane(Id.create(1, Lane.class));
		link1lane1.addToLinkId(Id.create(2, Link.class));
		link1lane1.setStartsAtMeterFromLinkEnd(105.0);
		link1lane1.setCapacityVehiclesPerHour(900.0);
		lanesForLink1.addLane(link1lane1);

		Lane link1lane2 = builder.createLane(Id.create(2, Lane.class));
		link1lane2.addToLinkId(Id.create(2, Link.class));
		link1lane2.setNumberOfRepresentedLanes(2);
		link1lane2.setStartsAtMeterFromLinkEnd(105.0);
		link1lane2.setCapacityVehiclesPerHour(1800.0);
		lanesForLink1.addLane(link1lane2);

		Lane link1lane3 = builder.createLane(Id.create(3, Lane.class));
		link1lane3.addToLinkId(Id.create(2, Link.class));
		link1lane3.setCapacityVehiclesPerHour(900.0);
		link1lane3.setStartsAtMeterFromLinkEnd(105.0);
		lanesForLink1.addLane(link1lane3);

		lanes.addLanesToLinkAssignment(lanesForLink1);
	}

	@Test
	void testCapacityWoLanes() {
		Config config = ConfigUtils.createConfig();
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		initNetwork(scenario.getNetwork());

		EventsManager eventsManager = EventsUtils.createEventsManager();
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim queueSim = new QSimBuilder(scenario.getConfig()) //
			.useDefaults() //
			.build(scenario, eventsManager);
		NetsimNetwork queueNetwork = queueSim.getNetsimNetwork();

		QLinkImpl ql = (QLinkImpl) queueNetwork.getNetsimLink(Id.create(1, Link.class));
		assertEquals(0.5, ql.getSimulatedFlowCapacityPerTimeStep(), 0);
		assertEquals(268.0, ql.getSpaceCap(), 0);
	}

	@Test
	void testCapacityWithOneLaneOneLane() {
		Config config = ConfigUtils.createConfig();
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		initNetwork(scenario.getNetwork());
		createOneLane(scenario, 1);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim queueSim = new QSimBuilder(scenario.getConfig()) //
			.useDefaults() //
			.build(scenario, eventsManager);
		NetsimNetwork queueNetwork = queueSim.getNetsimNetwork();

		// check link
		QLinkLanesImpl ql = (QLinkLanesImpl) queueNetwork.getNetsimLink(Id.create(1, Link.class));
		assertEquals(0.5, ql.getSimulatedFlowCapacity(), 0);
		//900 m link, 2 lanes = 240 storage + 105 m lane, 1 lane = 14 storage
		assertEquals(254.0, ql.getSpaceCap(), 0);

		//check original lane
		QLaneI qlane = ql.getOriginalLane();
		assertNotNull(qlane);
//		assertTrue(qlane.isFirstLaneOnLink());
		assertEquals(0.5, qlane.getSimulatedFlowCapacityPerTimeStep(), 0);
		assertEquals(240.0, qlane.getStorageCapacity(), 0);

		// check lane
		assertNotNull(ql.getOfferingQLanes());
		assertEquals(1, ql.getOfferingQLanes().size());
		qlane = ql.getOfferingQLanes().get(0);
		// lane cap = 900, no_represented_lanes = 1 -> lane flow = 0.25
		assertEquals(0.25, qlane.getSimulatedFlowCapacityPerTimeStep(), 0);
		assertEquals(14.0, qlane.getStorageCapacity(), 0);
	}

	@Test
	void testCapacityWithOneLaneTwoLanes() {
		Config config = ConfigUtils.createConfig();
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		initNetwork(scenario.getNetwork());
		createOneLane(scenario, 2);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim queueSim = new QSimBuilder(scenario.getConfig()) //
			.useDefaults() //
			.build(scenario, eventsManager);
		NetsimNetwork queueNetwork = queueSim.getNetsimNetwork();

		// check link
		QLinkLanesImpl ql = (QLinkLanesImpl) queueNetwork.getNetsimLink(Id.create(1, Link.class));
		assertEquals(0.5, ql.getSimulatedFlowCapacity(), 0);
		//900 m link, 2 lanes = 240 storage + 105 m lane, 2 lanes = 28 storage
		assertEquals(268.0, ql.getSpaceCap(), 0);

		//check original lane
		QLaneI qlane = ql.getOriginalLane();
		assertNotNull(qlane);
//		assertTrue(qlane.isFirstLaneOnLink());
		assertEquals(0.5, qlane.getSimulatedFlowCapacityPerTimeStep(), 0);
		assertEquals(240.0, qlane.getStorageCapacity(), 0);

		// check lane
		assertNotNull(ql.getOfferingQLanes());
		assertEquals(1, ql.getOfferingQLanes().size());
		qlane = ql.getOfferingQLanes().get(0);
		// lane cap = 900, no_represented_lanes = 2 -> lane flow = 0.5
		assertEquals(0.5, qlane.getSimulatedFlowCapacityPerTimeStep(), 0);
		assertEquals(28.0, qlane.getStorageCapacity(), 0);
	}


	@Test
	void testCapacityWithLanes() {
		Config config = ConfigUtils.createConfig();
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		initNetwork(scenario.getNetwork());
		createThreeLanes(scenario);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim queueSim = new QSimBuilder(scenario.getConfig()) //
			.useDefaults() //
			.build(scenario, eventsManager);
		NetsimNetwork queueNetwork = queueSim.getNetsimNetwork();

		// check link
		QLinkLanesImpl ql = (QLinkLanesImpl) queueNetwork.getNetsimLink(Id.create(1, Link.class));

		assertEquals(0.5, ql.getSimulatedFlowCapacity(), 0);

		/* 900 m link, 2 lanes = 240 storage
		 * + 105 m lane, 1 lane = 14 storage
		 * + 105 m lane, 2 lane = 28 storage
		 * + 105 m lane, 1 lane = 14 storage */
		assertEquals(296.0, ql.getSpaceCap(), 0);

		double totalStorageCapacity = 0.0;

		//check original lane
		QLaneI qlane = ql.getOriginalLane();
		assertNotNull(qlane);
//		assertTrue(qlane.isFirstLaneOnLink());
		assertEquals(0.5, qlane.getSimulatedFlowCapacityPerTimeStep(), 0);
		assertEquals(240.0, qlane.getStorageCapacity(), 0);
		totalStorageCapacity += qlane.getStorageCapacity();

		// check lanes
		assertNotNull(ql.getOfferingQLanes());
		assertEquals(3, ql.getOfferingQLanes().size());
		double totalFlowCapacity = 0.0;
		for (QLaneI qll : ql.getOfferingQLanes()) {
			if (((QueueWithBuffer)qll).getId().equals(Id.create(2, Lane.class))) {
				assertEquals(0.5, qll.getSimulatedFlowCapacityPerTimeStep(), 0);
				assertEquals(28.0, qll.getStorageCapacity(), 0);
			}
			else {
				assertEquals(0.25, qll.getSimulatedFlowCapacityPerTimeStep(), 0);
				assertEquals(14.0, qll.getStorageCapacity(), 0);
			}
			totalStorageCapacity += qll.getStorageCapacity();
			totalFlowCapacity += qll.getSimulatedFlowCapacityPerTimeStep();
		}
		assertEquals(ql.getSpaceCap(), totalStorageCapacity, 0);
		assertEquals(1.0, totalFlowCapacity, 0);
	}

}
