/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.queuesim;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.Events;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author dgrether
 */
public class QueueLinkTest extends MatsimTestCase {

	private Link link = null;
	private QueueNetwork queueNetwork = null;
	private QueueLink qlink = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		NetworkLayer network = new NetworkLayer();
		network.setCapacityPeriod(1.0);
		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(1, 0));
		this.link = network.createLink(new IdImpl("1"), node1, node2, 1.0, 1.0, 1.0, 1.0);
		super.loadConfig(null);
		this.queueNetwork = new QueueNetwork(network);
		this.qlink = this.queueNetwork.getQueueLink(new IdImpl("1"));
		this.qlink.finishInit();
	}

	@Override
	protected void tearDown() throws Exception {
		this.link = null;
		this.queueNetwork = null;
		this.qlink = null;
		super.tearDown();
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


	public void testAdd() {
		QueueVehicleImpl v = new QueueVehicleImpl(new IdImpl("1"));

		Person p = new PersonImpl(new IdImpl("1"));
		v.setDriver(new PersonAgent(p, null));
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
		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(1, 0));
		Node node3 = network.createNode(new IdImpl("3"), new CoordImpl(2, 0));
		Link link1 = network.createLink(new IdImpl("1"), node1, node2, 1.0, 1.0, 1.0, 1.0);
		Link link2 = network.createLink(new IdImpl("2"), node2, node3, 1.0, 1.0, 1.0, 1.0);
		this.queueNetwork = new QueueNetwork(network);
		this.qlink = this.queueNetwork.getQueueLink(new IdImpl("1"));
		this.qlink.finishInit();

		QueueSimulation qsim = new QueueSimulation(network, null, new Events());
		QueueVehicleImpl v1 = new QueueVehicleImpl(new IdImpl("1"));
		Person p = new PersonImpl(new IdImpl("1"));
		Plan plan = p.createPlan(true);
		try {
			plan.createActivity("h", link1);
			Leg leg = plan.createLeg(TransportMode.car);
			NetworkRoute route = (NetworkRoute) network.getFactory().createRoute(TransportMode.car, link1, link2);
			leg.setRoute(route);
			route.setLinks(link1, null, link2);
			leg.setRoute(route);
			plan.createActivity("w", link2);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		PersonAgent pa1 = new PersonAgent(p, qsim);
		v1.setDriver(pa1);
		pa1.setVehicle(v1);
		pa1.initialize();

		QueueVehicleImpl v2 = new QueueVehicleImpl(new IdImpl("2"));
		PersonAgent pa2 = new PersonAgent(p, qsim);
		v2.setDriver(pa2);
		pa2.setVehicle(v2);
		pa2.initialize();

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
		assertEquals(v1, this.qlink.getToNodeQueueLanes().get(0).popFirstFromBuffer());
		assertEquals(1, this.qlink.vehOnLinkCount());
		assertTrue(this.qlink.bufferIsEmpty());
		// time step 3, v2 moves to buffer
		this.qlink.moveLink(3.0);
		assertEquals(0, this.qlink.vehOnLinkCount());
		assertFalse(this.qlink.bufferIsEmpty());
		// v2 leaves buffer
		assertEquals(v2, this.qlink.getToNodeQueueLanes().get(0).popFirstFromBuffer());
		assertEquals(0, this.qlink.vehOnLinkCount());
		assertTrue(this.qlink.bufferIsEmpty());
		// time step 4, empty link
		this.qlink.moveLink(4.0);
		assertEquals(0, this.qlink.vehOnLinkCount());
		assertTrue(this.qlink.bufferIsEmpty());
	}
}
