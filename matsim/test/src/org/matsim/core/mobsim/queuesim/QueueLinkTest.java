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

import org.matsim.api.basic.v01.Id;
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
 * @author mrieser
 */
public class QueueLinkTest extends MatsimTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		loadConfig(null);
	}

	public void testInit() {
		Fixture f = new Fixture();
		assertNotNull(f.qlink);
		assertEquals(1.0, f.qlink.getSimulatedFlowCapacity(), EPSILON);
		assertEquals(1.0, f.qlink.getSpaceCap(), EPSILON);
		// TODO dg[april2008] this assertions are not covering everything in
		// QueueLink's constructor.
		// Extend the tests by checking the methods initFlowCapacity and
		// recalcCapacity
		assertEquals(f.link, f.qlink.getLink());
		assertEquals(f.queueNetwork.getQueueNode(new IdImpl("2")), f.qlink.getToQueueNode());
	}


	public void testAdd() {
		Fixture f = new Fixture();
		QueueVehicleImpl v = new QueueVehicleImpl(new IdImpl("1"));

		Person p = new PersonImpl(new IdImpl("1"));
		v.setDriver(new PersonAgent(p, null));
		Exception e = null;
		//as QueueLink has static access to the rest of the simulation
		//and testing other classes is not the purpose of this test
		//we have to do it like this
		//can be seen as reason why static access from an object should be avoided
		try {
			f.qlink.add(v);
		}
		catch (Exception ex) {
			e = ex;
		}
		assertNotNull(e);
		assertEquals(1, f.qlink.vehOnLinkCount());
		assertFalse(f.qlink.hasSpace());
		assertTrue(f.qlink.bufferIsEmpty());
	}

	/**
	 * Tests that vehicles driving on a link are found with {@link QueueLink#getVehicle(Id)}
	 * and {@link QueueLink#getAllVehicles()}. 
	 * 
	 * @author mrieser 
	 */
	public void testGetVehicle_Driving() {
		Fixture f = new Fixture();
		Id id1 = new IdImpl("1");

		QueueSimulation qsim = new QueueSimulation(f.network, null, new Events());

		QueueVehicle veh = new QueueVehicleImpl(id1);
		Person p = new PersonImpl(new IdImpl(23));
		veh.setDriver(new PersonAgent(p, qsim));

		// start test, check initial conditions
		assertTrue(f.qlink.bufferIsEmpty());
		assertEquals(0, f.qlink.vehOnLinkCount());
		assertNull(f.qlink.getVehicle(id1));
		assertEquals(0, f.qlink.getAllVehicles().size());

		// add a vehicle, it should be now in the vehicle queue
		f.qlink.add(veh);
		assertTrue(f.qlink.bufferIsEmpty());
		assertEquals(1, f.qlink.vehOnLinkCount());
		assertEquals("vehicle not found on link.", veh, f.qlink.getVehicle(id1));
		assertEquals(1, f.qlink.getAllVehicles().size());

		// time step 1, vehicle should be now in the buffer
		f.qlink.moveLink(1.0);
		assertFalse(f.qlink.bufferIsEmpty());
		assertEquals(0, f.qlink.vehOnLinkCount());
		assertEquals("vehicle not found in buffer.", veh, f.qlink.getVehicle(id1));
		assertEquals(1, f.qlink.getAllVehicles().size());
		assertEquals(veh, f.qlink.getAllVehicles().iterator().next());

		// time step 2, vehicle leaves link
		f.qlink.moveLink(2.0);
		assertEquals(veh, f.qlink.getToNodeQueueLanes().get(0).popFirstFromBuffer());
		assertTrue(f.qlink.bufferIsEmpty());
		assertEquals(0, f.qlink.vehOnLinkCount());
		assertNull("vehicle should not be on link anymore.", f.qlink.getVehicle(id1));
		assertEquals(0, f.qlink.getAllVehicles().size());
	}

	/**
	 * Tests that vehicles parked on a link are found with {@link QueueLink#getVehicle(Id)}
	 * and {@link QueueLink#getAllVehicles()}.
	 *  
	 * @author mrieser 
	 */
	public void testGetVehicle_Parking() {
		Fixture f = new Fixture();
		Id id2 = new IdImpl("2");

		QueueSimulation qsim = new QueueSimulation(f.network, null, new Events());

		QueueVehicle veh = new QueueVehicleImpl(id2);
		Person p = new PersonImpl(new IdImpl(42));
		veh.setDriver(new PersonAgent(p, qsim));

		// start test, check initial conditions
		assertTrue(f.qlink.bufferIsEmpty());
		assertEquals(0, f.qlink.vehOnLinkCount());
		assertEquals(0, f.qlink.getAllVehicles().size());

		f.qlink.addParkedVehicle(veh);
		assertTrue(f.qlink.bufferIsEmpty());
		assertEquals(0, f.qlink.vehOnLinkCount());
		assertEquals("vehicle not found in parking list.", veh, f.qlink.getVehicle(id2));
		assertEquals(1, f.qlink.getAllVehicles().size());
		assertEquals(veh, f.qlink.getAllVehicles().iterator().next());

		assertEquals("removed wrong vehicle.", veh, f.qlink.removeParkedVehicle(veh.getId()));
		assertTrue(f.qlink.bufferIsEmpty());
		assertEquals(0, f.qlink.vehOnLinkCount());
		assertNull("vehicle not found in parking list.", f.qlink.getVehicle(id2));
		assertEquals(0, f.qlink.getAllVehicles().size());
	}

	/**
	 * Tests that vehicles departing on a link are found with {@link QueueLink#getVehicle(Id)}
	 * and {@link QueueLink#getAllVehicles()}.
	 * 
	 * @author mrieser 
	 */
	public void testGetVehicle_Departing() {
		Fixture f = new Fixture();
		Id id3 = new IdImpl("3");

		QueueSimulation qsim = new QueueSimulation(f.network, null, new Events());

		QueueVehicle veh = new QueueVehicleImpl(id3);
		Person p = new PersonImpl(new IdImpl(80));
		veh.setDriver(new PersonAgent(p, qsim));

		// start test, check initial conditions
		assertTrue(f.qlink.bufferIsEmpty());
		assertEquals(0, f.qlink.vehOnLinkCount());
		assertEquals(0, f.qlink.getAllVehicles().size());

		f.qlink.addDepartingVehicle(veh);
		assertTrue(f.qlink.bufferIsEmpty());
		assertEquals(0, f.qlink.vehOnLinkCount());
		assertEquals("vehicle not found in waiting list.", veh, f.qlink.getVehicle(id3));
		assertEquals(1, f.qlink.getAllVehicles().size());
		assertEquals(veh, f.qlink.getAllVehicles().iterator().next());

		// time step 1, vehicle should be now in the buffer
		f.qlink.moveLink(1.0);
		assertFalse(f.qlink.bufferIsEmpty());
		assertEquals(0, f.qlink.vehOnLinkCount());
		assertEquals("vehicle not found in buffer.", veh, f.qlink.getVehicle(id3));
		assertEquals(1, f.qlink.getAllVehicles().size());

		// vehicle leaves link
		assertEquals(veh, f.qlink.getToNodeQueueLanes().get(0).popFirstFromBuffer());
		assertTrue(f.qlink.bufferIsEmpty());
		assertEquals(0, f.qlink.vehOnLinkCount());
		assertNull("vehicle should not be on link anymore.", f.qlink.getVehicle(id3));
		assertEquals(0, f.qlink.getAllVehicles().size());
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
		QueueNetwork queueNetwork = new QueueNetwork(network);
		QueueLink qlink = queueNetwork.getQueueLink(new IdImpl("1"));
		qlink.finishInit();

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
		assertTrue(qlink.bufferIsEmpty());
		assertEquals(0, qlink.vehOnLinkCount());
		// add v1
		qlink.add(v1);
		assertEquals(1, qlink.vehOnLinkCount());
		assertTrue(qlink.bufferIsEmpty());
		// time step 1, v1 is moved to buffer
		qlink.moveLink(1.0);
		assertEquals(0, qlink.vehOnLinkCount());
		assertFalse(qlink.bufferIsEmpty());
		// add v2, still time step 1
		qlink.add(v2);
		assertEquals(1, qlink.vehOnLinkCount());
		assertFalse(qlink.bufferIsEmpty());
		// time step 2, v1 still in buffer, v2 cannot enter buffer, so still on link
		qlink.moveLink(2.0);
		assertEquals(1, qlink.vehOnLinkCount());
		assertFalse(qlink.bufferIsEmpty());
		// v1 leaves buffer
		assertEquals(v1, qlink.getToNodeQueueLanes().get(0).popFirstFromBuffer());
		assertEquals(1, qlink.vehOnLinkCount());
		assertTrue(qlink.bufferIsEmpty());
		// time step 3, v2 moves to buffer
		qlink.moveLink(3.0);
		assertEquals(0, qlink.vehOnLinkCount());
		assertFalse(qlink.bufferIsEmpty());
		// v2 leaves buffer
		assertEquals(v2, qlink.getToNodeQueueLanes().get(0).popFirstFromBuffer());
		assertEquals(0, qlink.vehOnLinkCount());
		assertTrue(qlink.bufferIsEmpty());
		// time step 4, empty link
		qlink.moveLink(4.0);
		assertEquals(0, qlink.vehOnLinkCount());
		assertTrue(qlink.bufferIsEmpty());
	}

	/**
	 * Initializes some commonly used data in the tests.
	 *
	 * @author mrieser
	 */
	private static final class Fixture {
		/*package*/ final Link link;
		/*package*/ final NetworkLayer network;
		/*package*/ final QueueNetwork queueNetwork;
		/*package*/ final QueueLink qlink;

		/*package*/ Fixture() {
			this.network = new NetworkLayer();
			this.network.setCapacityPeriod(1.0);
			Node node1 = this.network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
			Node node2 = this.network.createNode(new IdImpl("2"), new CoordImpl(1, 0));
			this.link = this.network.createLink(new IdImpl("1"), node1, node2, 1.0, 1.0, 1.0, 1.0);
			this.queueNetwork = new QueueNetwork(this.network);
			this.qlink = this.queueNetwork.getQueueLink(new IdImpl("1"));
			this.qlink.finishInit();
		}

	}

}
