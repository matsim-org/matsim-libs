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
package org.matsim.ptproject.qsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LaneDefinitionsFactory;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.testcases.MatsimTestCase;


/**
 * Test for QueueLane capacity calculations
 * @author dgrether
 *
 */
public class QueueLaneTest extends MatsimTestCase {

	private final Id id1 = new IdImpl("1");
  private final Id id2 = new IdImpl("2");	
  private final Id id3 = new IdImpl("3");	
	
  private Network initNetwork(NetworkImpl network) {
		network.setCapacityPeriod(3600.0);
		Node node1 = network.getFactory().createNode(id1, new CoordImpl(0, 0));
		Node node2 = network.getFactory().createNode(id2, new CoordImpl(1, 0));
		Node node3 = network.getFactory().createNode(id3, new CoordImpl(2, 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		Link l1 = network.getFactory().createLink(id1, id1, id2);
		l1.setLength(1005.0);
		l1.setFreespeed(15.0);
		l1.setCapacity(1800.0);
		l1.setNumberOfLanes(2.0);
		network.addLink(l1);
		Link l2 = network.getFactory().createLink(id2, id2, id3);
		network.addLink(l2);
		return network;
  }
  
	private LaneDefinitions createOneLane(ScenarioImpl scenario, int numberOfRepresentedLanes) {
		scenario.getConfig().scenario().setUseLanes(true);
		LaneDefinitions lanes = scenario.getLaneDefinitions();
		LaneDefinitionsFactory builder = lanes.getFactory();
		//lanes for link 1
		LanesToLinkAssignment lanesForLink1 = builder.createLanesToLinkAssignment(id1);
		Lane link1lane1 = builder.createLane(id1);
		link1lane1.addToLinkId(id2);
		link1lane1.setLength(105.0);
		link1lane1.setNumberOfRepresentedLanes(numberOfRepresentedLanes);
		lanesForLink1.addLane(link1lane1);
		lanes.addLanesToLinkAssignment(lanesForLink1);
		return lanes;
	}
  
	private LaneDefinitions createLanes(ScenarioImpl scenario) {
		scenario.getConfig().scenario().setUseLanes(true);
		LaneDefinitions lanes = scenario.getLaneDefinitions();
		LaneDefinitionsFactory builder = lanes.getFactory();
		//lanes for link 1
		LanesToLinkAssignment lanesForLink1 = builder.createLanesToLinkAssignment(id1);
		Lane link1lane1 = builder.createLane(id1);
		link1lane1.addToLinkId(id2);
		link1lane1.setLength(105.0);
		lanesForLink1.addLane(link1lane1);
		
		Lane link1lane2 = builder.createLane(id2);
		link1lane2.addToLinkId(id2);
		link1lane2.setNumberOfRepresentedLanes(2);
		link1lane2.setLength(105.0);
		lanesForLink1.addLane(link1lane2);

		Lane link1lane3 = builder.createLane(id3);
		link1lane3.addToLinkId(id2);
		link1lane3.setLength(105.0);
		lanesForLink1.addLane(link1lane3);
		
		lanes.addLanesToLinkAssignment(lanesForLink1);
		return lanes;
	}
  	
  
	public void testCapacityWoLanes() {
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		
		Network network = this.initNetwork(scenario.getNetwork());
		
		QueueSimulation queueSim = new QueueSimulation(network, null, null);
		QueueNetwork queueNetwork = queueSim.getQueueNetwork();
		QueueLink ql = queueNetwork.getQueueLink(id1);

		assertEquals(0.5, ql.getSimulatedFlowCapacity());
		assertEquals(268.0, ql.getSpaceCap());
	}
	
	public void testCapacityWithOneLaneOneLane() {
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		Network network = this.initNetwork(scenario.getNetwork());
		LaneDefinitions lanes = this.createOneLane(scenario, 1);
		
		QueueSimulation queueSim = new QueueSimulation(network, null, null);
		QueueNetwork queueNetwork = queueSim.getQueueNetwork();
		QueueLink ql = queueNetwork.getQueueLink(id1);

		queueSim.setLaneDefinitions(lanes);
		queueSim.prepareLanes();
		assertEquals(0.5, ql.getSimulatedFlowCapacity());
		//900 m link, 2 lanes = 240 storage + 105 m lane, 1 lane = 14 storage
		assertEquals(254.0, ql.getSpaceCap());
		//check original lane
		QueueLane qlane = ql.getOriginalLane();
		assertNotNull(qlane);
		assertTrue(qlane.isOriginalLane());
		assertEquals(0.5, qlane.getSimulatedFlowCapacity());
		assertEquals(240.0, qlane.getStorageCapacity());
		
		// check lane
		assertNotNull(ql.getToNodeQueueLanes());
		assertEquals(1, ql.getToNodeQueueLanes().size());
		qlane = ql.getToNodeQueueLanes().get(0);
		
		// link_no_of_lanes = 2 flow = 0.5 -> lane_flow = 0.5/2 * 1 = 0.25
		assertEquals(0.25, qlane.getSimulatedFlowCapacity());
		assertEquals(14.0, qlane.getStorageCapacity());
	}

	public void testCapacityWithOneLaneOneLaneTwoLanes() {
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		Network network = this.initNetwork(scenario.getNetwork());
		LaneDefinitions lanes = this.createOneLane(scenario, 2);
		
		QueueSimulation queueSim = new QueueSimulation(network, null, null);
		QueueNetwork queueNetwork = queueSim.getQueueNetwork();
		QueueLink ql = queueNetwork.getQueueLink(id1);

		queueSim.setLaneDefinitions(lanes);
		queueSim.prepareLanes();
		assertEquals(0.5, ql.getSimulatedFlowCapacity());
		//900 m link, 2 lanes = 240 storage + 105 m lane, 2 lanes = 28 storage
		assertEquals(268.0, ql.getSpaceCap());
		//check original lane
		QueueLane qlane = ql.getOriginalLane();
		assertNotNull(qlane);
		assertTrue(qlane.isOriginalLane());
		assertEquals(0.5, qlane.getSimulatedFlowCapacity());
		assertEquals(240.0, qlane.getStorageCapacity());
		
		// check lane
		assertNotNull(ql.getToNodeQueueLanes());
		assertEquals(1, ql.getToNodeQueueLanes().size());
		qlane = ql.getToNodeQueueLanes().get(0);
		
		// link_no_of_lanes = 2 flow = 0.5 -> lane_flow = 0.5/2 * 2 = 0.5
		assertEquals(0.5, qlane.getSimulatedFlowCapacity());
		assertEquals(28.0, qlane.getStorageCapacity());
	}

	
	
	public void testCapacityWithLanes() {
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		Network network = this.initNetwork(scenario.getNetwork());
		LaneDefinitions lanes = this.createLanes(scenario);
		
		QueueSimulation queueSim = new QueueSimulation(network, null, null);
		QueueNetwork queueNetwork = queueSim.getQueueNetwork();
		QueueLink ql = queueNetwork.getQueueLink(id1);

		queueSim.setLaneDefinitions(lanes);
		queueSim.prepareLanes();
		assertEquals(0.5, ql.getSimulatedFlowCapacity());
		//240 link + 2 * 14 + 1 * 28 = 
		assertEquals(296.0, ql.getSpaceCap());
		double totalStorageCapacity = 0.0;
		//check original lane
		QueueLane qlane = ql.getOriginalLane();
		assertNotNull(qlane);
		assertTrue(qlane.isOriginalLane());
		assertEquals(0.5, qlane.getSimulatedFlowCapacity());
		assertEquals(240.0, qlane.getStorageCapacity());
		totalStorageCapacity += qlane.getStorageCapacity();
		// check lanes
		assertNotNull(ql.getToNodeQueueLanes());
		assertEquals(3, ql.getToNodeQueueLanes().size());
		double totalFlowCapacity = 0.0;
		for (QueueLane qll : ql.getToNodeQueueLanes()) {
			if (qll.getLaneId().equals(id2)) {
				assertEquals(0.5, qll.getSimulatedFlowCapacity());
				assertEquals(28.0, qll.getStorageCapacity());
			}
			else {
				assertEquals(0.25, qll.getSimulatedFlowCapacity());
				assertEquals(14.0, qll.getStorageCapacity());
			}
			totalStorageCapacity += qll.getStorageCapacity();
			totalFlowCapacity += qll.getSimulatedFlowCapacity();
		}
		assertEquals(ql.getSpaceCap(), totalStorageCapacity);
		assertEquals(1.0, totalFlowCapacity);
	}
	
	
}
