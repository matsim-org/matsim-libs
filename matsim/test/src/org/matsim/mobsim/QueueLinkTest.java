/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.mobsim;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Route;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author dgrether
 *
 */
public class QueueLinkTest extends MatsimTestCase {

	private NetworkLayer network;

	private Node node1;

	private Node node2;

	private Link link;

	private QueueNetwork queueNetwork;

	private QueueLink qlink;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.network = new NetworkLayer();
		this.network.setCapacityPeriod(1.0);
		this.node1 = this.network.createNode("1", "0", "0", null);
		this.node2 = this.network.createNode("2", "1", "0", null);
		this.link = this.network.createLink("1", "1", "2", "1", "1",
				"1", "1", null, null);
		super.loadConfig(null);
		this.queueNetwork = new QueueNetwork(this.network);
		this.qlink = this.queueNetwork.getQueueLink(new IdImpl("1"));
		this.qlink.finishInit();
	}

	public void testInit() {
		assertNotNull(this.qlink);
		assertEquals(1.0, this.qlink.getSimulatedFlowCapacity(), EPSILON);
		assertEquals(1.0, this.qlink.getSpaceCap(), EPSILON);
		// TODO dg[april2008] this assertions are not covering everything in
		// QueueLink's constructor.
		// Extend the tests by checking the methods initFlowCapacity and
		// recalcCapacity
		assertEquals(this.link, this.qlink.getLink());
		assertEquals(this.queueNetwork.getQueueNode(new IdImpl("2")), this.qlink
				.getToQueueNode());
	}

	public void testChangeSimulatedFlowCapacity() {
		assertEquals(1.0, this.qlink.getSimulatedFlowCapacity(), EPSILON);
		this.qlink.scaleSimulatedFlowCapacity(2.0);
		assertEquals(2.0, this.qlink.getSimulatedFlowCapacity(), EPSILON);
		this.qlink.scaleSimulatedFlowCapacity(0.5);
		assertEquals(1.0, this.qlink.getSimulatedFlowCapacity(), EPSILON);
	}

	public void testAdd() {
		Vehicle v = new Vehicle();

		Person p = new Person(new IdImpl("1"));
		v.setDriver(p);
		Exception e = null;
		//as QueueLink has static access to the rest of the simulation
		//and testing other classes is not the purpose of this test
		//we have to do it like this
		//can be seen as reason why static access from an object should be avoided
		try {
			this.qlink.add(v);
		}
		catch (Exception ex) {
			e = ex;
		}
		assertNotNull(e);
		assertEquals(1, this.qlink.vehOnLinkCount());
		assertFalse(this.qlink.hasSpace());
		assertTrue(this.qlink.bufferIsEmpty());
	}

	/**
	 * Tests the behavior of the buffer (e.g. that it does not accept too many vehicles).
	 *
	 * @author mrieser
	 */
	public void testBuffer() {
		NetworkLayer network = new NetworkLayer();
		network.setCapacityPeriod(1.0);
		network.createNode("1", "0", "0", null);
		network.createNode("2", "1", "0", null);
		network.createNode("3", "2", "0", null);
		Link link1 = network.createLink("1", "1", "2", "1", "1", "1", "1", null, null);
		Link link2 = network.createLink("2", "2", "3", "1", "1", "1", "1", null, null);
		Gbl.createWorld().setNetworkLayer(network);
		this.queueNetwork = new QueueNetwork(network);
		this.qlink = this.queueNetwork.getQueueLink(new IdImpl("1"));
		this.qlink.finishInit();

		new QueueSimulation(this.network, null, new Events());
		Vehicle v1 = new Vehicle();
		Person p = new Person(new IdImpl("1"));
		Plan plan = p.createPlan(true);
		try {
			plan.createAct("h", 0.0, 0.0, link1, 0.0, 0.0, 0.0, false);
			Leg leg = plan.createLeg("car", 0.0, 1.0, 1.0);
			Route route = leg.createRoute("1", "00:00:01");
			route.setRoute("2");
			leg.setRoute(route);
			plan.createAct("w", 0.0, 0.0, link2, 0.0, 0.0, 0.0, false);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		v1.setDriver(p);
		v1.setActLegs(p.getPlans().get(0).getActsLegs());
		v1.initVeh();
		Vehicle v2 = new Vehicle();
		v2.setDriver(p);
		v2.setActLegs(p.getPlans().get(0).getActsLegs());
		v2.initVeh();

		// start test
		assertTrue(this.qlink.bufferIsEmpty());
		assertEquals(0, this.qlink.vehOnLinkCount());
		// add v1
		this.qlink.add(v1);
		assertEquals(1, this.qlink.vehOnLinkCount());
		assertTrue(this.qlink.bufferIsEmpty());
		// time step 1, v1 is moved to buffer
		this.qlink.moveLink(1.0);
		assertEquals(0, this.qlink.vehOnLinkCount());
		assertFalse(this.qlink.bufferIsEmpty());
		// add v2, still time step 1
		this.qlink.add(v2);
		assertEquals(1, this.qlink.vehOnLinkCount());
		assertFalse(this.qlink.bufferIsEmpty());
		// time step 2, v1 still in buffer, v2 cannot enter buffer, so still on link
		this.qlink.moveLink(2.0);
		assertEquals(1, this.qlink.vehOnLinkCount());
		assertFalse(this.qlink.bufferIsEmpty());
		// v1 leaves buffer
		assertEquals(v1, this.qlink.popFirstFromBuffer());
		assertEquals(1, this.qlink.vehOnLinkCount());
		assertTrue(this.qlink.bufferIsEmpty());
		// time step 3, v2 moves to buffer
		this.qlink.moveLink(3.0);
		assertEquals(0, this.qlink.vehOnLinkCount());
		assertFalse(this.qlink.bufferIsEmpty());
		// v2 leaves buffer
		assertEquals(v2, this.qlink.popFirstFromBuffer());
		assertEquals(0, this.qlink.vehOnLinkCount());
		assertTrue(this.qlink.bufferIsEmpty());
		// time step 4, empty link
		this.qlink.moveLink(4.0);
		assertEquals(0, this.qlink.vehOnLinkCount());
		assertTrue(this.qlink.bufferIsEmpty());
	}
}
