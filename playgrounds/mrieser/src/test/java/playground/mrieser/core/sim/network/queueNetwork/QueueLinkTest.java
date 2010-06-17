/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.sim.network.queueNetwork;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleTypeImpl;

import playground.mrieser.core.sim.api.SimVehicle;
import playground.mrieser.core.sim.fakes.FakeSimEngine;
import playground.mrieser.core.sim.impl.DefaultSimVehicle;
import playground.mrieser.core.sim.network.api.SimLink;

/**
 * @author mrieser
 */
public class QueueLinkTest {

	@Test
	public void testInsertVehicle_atParking() {
		NetworkLayer net = new NetworkLayer();
		net.addNode(net.getFactory().createNode(new IdImpl(1), new CoordImpl(0, 0)));
		net.addNode(net.getFactory().createNode(new IdImpl(2), new CoordImpl(1000, 0)));
		Link link = net.getFactory().createLink(new IdImpl(5), new IdImpl(1), new IdImpl(2));
		link.setLength(1000);
		link.setFreespeed(10);
		link.setCapacity(3600.0);
		link.setNumberOfLanes(1.0);
		QueueNetwork qnet = new QueueNetwork(new FakeSimEngine());
		QueueLink ql = new QueueLink(link, qnet);

		Id veh1Id = new IdImpl(11);
		SimVehicle vehicle1 = new DefaultSimVehicle(new VehicleImpl(veh1Id, new VehicleTypeImpl(new IdImpl("1980"))));

		Assert.assertNull(ql.getParkedVehicle(veh1Id));
		ql.insertVehicle(vehicle1, SimLink.POSITION_AT_FROM_NODE, SimLink.PRIORITY_PARKING);
		Assert.assertEquals(vehicle1, ql.getParkedVehicle(veh1Id));
		Assert.assertNull(ql.getParkedVehicle(new IdImpl("1980")));

		Id veh2Id = new IdImpl(5);
		SimVehicle vehicle2 = new DefaultSimVehicle(new VehicleImpl(veh2Id, new VehicleTypeImpl(new IdImpl("1979"))));

		Assert.assertNull(ql.getParkedVehicle(veh2Id));
		ql.insertVehicle(vehicle2, SimLink.POSITION_AT_FROM_NODE, SimLink.PRIORITY_PARKING);
		Assert.assertEquals(vehicle2, ql.getParkedVehicle(veh2Id));
		Assert.assertEquals(vehicle1, ql.getParkedVehicle(veh1Id));
		Assert.assertNull(ql.getParkedVehicle(new IdImpl("1979")));
	}

	@Test
	public void testRemoveVehicle_atParking() {
		NetworkLayer net = new NetworkLayer();
		net.addNode(net.getFactory().createNode(new IdImpl(1), new CoordImpl(0, 0)));
		net.addNode(net.getFactory().createNode(new IdImpl(2), new CoordImpl(1000, 0)));
		Link link = net.getFactory().createLink(new IdImpl(5), new IdImpl(1), new IdImpl(2));
		link.setLength(1000);
		link.setFreespeed(10);
		link.setCapacity(3600.0);
		link.setNumberOfLanes(1.0);
		QueueNetwork qnet = new QueueNetwork(new FakeSimEngine());
		QueueLink ql = new QueueLink(link, qnet);

		Id veh1Id = new IdImpl(11);
		SimVehicle vehicle1 = new DefaultSimVehicle(new VehicleImpl(veh1Id, new VehicleTypeImpl(new IdImpl("1980"))));
		Id veh2Id = new IdImpl(5);
		SimVehicle vehicle2 = new DefaultSimVehicle(new VehicleImpl(veh2Id, new VehicleTypeImpl(new IdImpl("1979"))));
		ql.insertVehicle(vehicle1, SimLink.POSITION_AT_FROM_NODE, SimLink.PRIORITY_PARKING);
		ql.insertVehicle(vehicle2, SimLink.POSITION_AT_FROM_NODE, SimLink.PRIORITY_PARKING);

		Assert.assertEquals(vehicle1, ql.getParkedVehicle(veh1Id));
		Assert.assertEquals(vehicle2, ql.getParkedVehicle(veh2Id));

		ql.removeVehicle(vehicle1);

		Assert.assertNull(ql.getParkedVehicle(veh1Id));
		Assert.assertEquals(vehicle2, ql.getParkedVehicle(veh2Id));

		ql.removeVehicle(vehicle1); // the same again, shouldn't do any harm

		Assert.assertNull(ql.getParkedVehicle(veh1Id));
		Assert.assertEquals(vehicle2, ql.getParkedVehicle(veh2Id));

		ql.removeVehicle(vehicle2); // the same again, shouldn't do any harm

		Assert.assertNull(ql.getParkedVehicle(veh1Id));
		Assert.assertNull(ql.getParkedVehicle(veh2Id));
	}

	@Test
	public void testRemoveVehicle_driving() {
		NetworkLayer net = new NetworkLayer();
		net.addNode(net.getFactory().createNode(new IdImpl(1), new CoordImpl(0, 0)));
		net.addNode(net.getFactory().createNode(new IdImpl(2), new CoordImpl(1000, 0)));
		Link link = net.getFactory().createLink(new IdImpl(5), new IdImpl(1), new IdImpl(2));
		link.setLength(1000);
		link.setFreespeed(10);
		link.setCapacity(3600.0);
		link.setNumberOfLanes(1.0);
		FakeSimEngine engine = new FakeSimEngine();
		QueueNetwork qnet = new QueueNetwork(engine);
		QueueLink ql = new QueueLink(link, qnet);

		Id veh1Id = new IdImpl(11);
		SimVehicle vehicle1 = new DefaultSimVehicle(new VehicleImpl(veh1Id, new VehicleTypeImpl(new IdImpl("1980"))));

		engine.setCurrentTime(200);
		ql.addVehicle(vehicle1);
		ql.doSimStep(250);
		ql.removeVehicle(vehicle1);
		ql.doSimStep(300); // vehicle1 should not show up in buffer
		Assert.assertNull(ql.getFirstVehicleInBuffer());
		ql.doSimStep(310);
		Assert.assertNull(ql.getFirstVehicleInBuffer());
	}

	@Test
	public void testRemoveVehicle_atWaiting() {
		NetworkLayer net = new NetworkLayer();
		net.addNode(net.getFactory().createNode(new IdImpl(1), new CoordImpl(0, 0)));
		net.addNode(net.getFactory().createNode(new IdImpl(2), new CoordImpl(1000, 0)));
		Link link = net.getFactory().createLink(new IdImpl(5), new IdImpl(1), new IdImpl(2));
		link.setLength(1000);
		link.setFreespeed(10);
		link.setCapacity(3600.0);
		link.setNumberOfLanes(1.0);
		FakeSimEngine engine = new FakeSimEngine();
		QueueNetwork qnet = new QueueNetwork(engine);
		QueueLink ql = new QueueLink(link, qnet);

		Id veh1Id = new IdImpl(11);
		SimVehicle vehicle1 = new DefaultSimVehicle(new VehicleImpl(veh1Id, new VehicleTypeImpl(new IdImpl("1980"))));
		Id veh2Id = new IdImpl(22);
		SimVehicle vehicle2 = new DefaultSimVehicle(new VehicleImpl(veh2Id, new VehicleTypeImpl(new IdImpl("1980"))));

		engine.setCurrentTime(200);
		ql.insertVehicle(vehicle1, SimLink.POSITION_AT_TO_NODE, SimLink.PRIORITY_AS_SOON_AS_SPACE_AVAILABLE);
		ql.insertVehicle(vehicle2, SimLink.POSITION_AT_TO_NODE, SimLink.PRIORITY_AS_SOON_AS_SPACE_AVAILABLE);
		ql.doSimStep(201);
		Assert.assertEquals(vehicle1, ql.getFirstVehicleInBuffer());
		ql.removeVehicle(vehicle2);
		ql.doSimStep(202); // vehicle1 should not show up in buffer
		Assert.assertEquals(vehicle1, ql.removeFirstVehicleInBuffer());
		Assert.assertNull(ql.getFirstVehicleInBuffer());
		ql.doSimStep(310);
		Assert.assertNull(ql.getFirstVehicleInBuffer());
	}

	@Test
	public void testRemoveVehicle_fromBuffer() {
		NetworkLayer net = new NetworkLayer();
		net.addNode(net.getFactory().createNode(new IdImpl(1), new CoordImpl(0, 0)));
		net.addNode(net.getFactory().createNode(new IdImpl(2), new CoordImpl(1000, 0)));
		Link link = net.getFactory().createLink(new IdImpl(5), new IdImpl(1), new IdImpl(2));
		link.setLength(1000);
		link.setFreespeed(10);
		link.setCapacity(3600.0);
		link.setNumberOfLanes(1.0);
		FakeSimEngine engine = new FakeSimEngine();
		QueueNetwork qnet = new QueueNetwork(engine);
		QueueLink ql = new QueueLink(link, qnet);

		Id veh1Id = new IdImpl(11);
		SimVehicle vehicle1 = new DefaultSimVehicle(new VehicleImpl(veh1Id, new VehicleTypeImpl(new IdImpl("1980"))));

		engine.setCurrentTime(200);
		ql.addVehicle(vehicle1);
		ql.doSimStep(250);
		ql.doSimStep(300); // vehicle1 should not show up in buffer
		Assert.assertEquals(vehicle1, ql.getFirstVehicleInBuffer());
		ql.removeVehicle(vehicle1);
		Assert.assertNull(ql.getFirstVehicleInBuffer());
	}

	@Test
	public void test_FreeflowSpeed_DrivingSingleVehicle() {
		NetworkLayer net = new NetworkLayer();
		net.addNode(net.getFactory().createNode(new IdImpl(1), new CoordImpl(0, 0)));
		net.addNode(net.getFactory().createNode(new IdImpl(2), new CoordImpl(1000, 0)));
		Link link = net.getFactory().createLink(new IdImpl(5), new IdImpl(1), new IdImpl(2));
		link.setLength(1000);
		link.setFreespeed(10);
		link.setCapacity(3600.0);
		link.setNumberOfLanes(1.0);
		FakeSimEngine engine = new FakeSimEngine();
		QueueNetwork qnet = new QueueNetwork(engine);
		QueueLink ql = new QueueLink(link, qnet);

		Id veh1Id = new IdImpl(11);
		SimVehicle vehicle1 = new DefaultSimVehicle(new VehicleImpl(veh1Id, new VehicleTypeImpl(new IdImpl("1980"))));

		engine.setCurrentTime(200);
		ql.addVehicle(vehicle1);
		Assert.assertNull(ql.getFirstVehicleInBuffer());
		ql.doSimStep(201);
		Assert.assertNull(ql.getFirstVehicleInBuffer());
		ql.doSimStep(202);
		Assert.assertNull(ql.getFirstVehicleInBuffer());
		ql.doSimStep(250);
		Assert.assertNull(ql.getFirstVehicleInBuffer());
		ql.doSimStep(299);
		Assert.assertNull(ql.getFirstVehicleInBuffer());
		ql.doSimStep(300);
		Assert.assertEquals(vehicle1, ql.getFirstVehicleInBuffer());
		ql.doSimStep(301);
		Assert.assertEquals(vehicle1, ql.getFirstVehicleInBuffer());
	}
}
