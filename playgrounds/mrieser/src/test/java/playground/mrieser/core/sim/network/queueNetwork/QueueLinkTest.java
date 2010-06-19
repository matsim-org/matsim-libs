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

import java.util.Random;

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
		Fixture f = new Fixture();

		Id veh1Id = new IdImpl(11);
		SimVehicle vehicle1 = new DefaultSimVehicle(new VehicleImpl(veh1Id, new VehicleTypeImpl(new IdImpl("1980"))));

		Assert.assertNull(f.qlink.getParkedVehicle(veh1Id));
		f.qlink.insertVehicle(vehicle1, SimLink.POSITION_AT_FROM_NODE, SimLink.PRIORITY_PARKING);
		Assert.assertEquals(vehicle1, f.qlink.getParkedVehicle(veh1Id));
		Assert.assertNull(f.qlink.getParkedVehicle(new IdImpl("1980")));

		Id veh2Id = new IdImpl(5);
		SimVehicle vehicle2 = new DefaultSimVehicle(new VehicleImpl(veh2Id, new VehicleTypeImpl(new IdImpl("1979"))));

		Assert.assertNull(f.qlink.getParkedVehicle(veh2Id));
		f.qlink.insertVehicle(vehicle2, SimLink.POSITION_AT_FROM_NODE, SimLink.PRIORITY_PARKING);
		Assert.assertEquals(vehicle2, f.qlink.getParkedVehicle(veh2Id));
		Assert.assertEquals(vehicle1, f.qlink.getParkedVehicle(veh1Id));
		Assert.assertNull(f.qlink.getParkedVehicle(new IdImpl("1979")));
	}

	@Test
	public void testRemoveVehicle_atParking() {
		Fixture f = new Fixture();

		Id veh1Id = new IdImpl(11);
		SimVehicle vehicle1 = new DefaultSimVehicle(new VehicleImpl(veh1Id, new VehicleTypeImpl(new IdImpl("1980"))));
		Id veh2Id = new IdImpl(5);
		SimVehicle vehicle2 = new DefaultSimVehicle(new VehicleImpl(veh2Id, new VehicleTypeImpl(new IdImpl("1979"))));
		f.qlink.insertVehicle(vehicle1, SimLink.POSITION_AT_FROM_NODE, SimLink.PRIORITY_PARKING);
		f.qlink.insertVehicle(vehicle2, SimLink.POSITION_AT_FROM_NODE, SimLink.PRIORITY_PARKING);

		Assert.assertEquals(vehicle1, f.qlink.getParkedVehicle(veh1Id));
		Assert.assertEquals(vehicle2, f.qlink.getParkedVehicle(veh2Id));

		f.qlink.removeVehicle(vehicle1);

		Assert.assertNull(f.qlink.getParkedVehicle(veh1Id));
		Assert.assertEquals(vehicle2, f.qlink.getParkedVehicle(veh2Id));

		f.qlink.removeVehicle(vehicle1); // the same again, shouldn't do any harm

		Assert.assertNull(f.qlink.getParkedVehicle(veh1Id));
		Assert.assertEquals(vehicle2, f.qlink.getParkedVehicle(veh2Id));

		f.qlink.removeVehicle(vehicle2); // the same again, shouldn't do any harm

		Assert.assertNull(f.qlink.getParkedVehicle(veh1Id));
		Assert.assertNull(f.qlink.getParkedVehicle(veh2Id));
	}

	@Test
	public void testRemoveVehicle_driving() {
		Fixture f = new Fixture();

		Id veh1Id = new IdImpl(11);
		SimVehicle vehicle1 = new DefaultSimVehicle(new VehicleImpl(veh1Id, new VehicleTypeImpl(new IdImpl("1980"))));

		f.engine.setCurrentTime(200);
		f.qlink.addVehicle(vehicle1);
		f.qlink.doSimStep(250);
		f.qlink.removeVehicle(vehicle1);
		f.qlink.doSimStep(300); // vehicle1 should not show up in buffer
		Assert.assertNull(f.qlink.buffer.getFirstVehicleInBuffer());
		f.qlink.doSimStep(310);
		Assert.assertNull(f.qlink.buffer.getFirstVehicleInBuffer());
	}

	@Test
	public void testRemoveVehicle_atWaiting() {
		Fixture f = new Fixture();

		Id veh1Id = new IdImpl(11);
		SimVehicle vehicle1 = new DefaultSimVehicle(new VehicleImpl(veh1Id, new VehicleTypeImpl(new IdImpl("1980"))));
		Id veh2Id = new IdImpl(22);
		SimVehicle vehicle2 = new DefaultSimVehicle(new VehicleImpl(veh2Id, new VehicleTypeImpl(new IdImpl("1980"))));

		f.engine.setCurrentTime(200);
		f.qlink.insertVehicle(vehicle1, SimLink.POSITION_AT_TO_NODE, SimLink.PRIORITY_AS_SOON_AS_SPACE_AVAILABLE);
		f.qlink.insertVehicle(vehicle2, SimLink.POSITION_AT_TO_NODE, SimLink.PRIORITY_AS_SOON_AS_SPACE_AVAILABLE);
		f.qlink.doSimStep(201);
		Assert.assertEquals(vehicle1, f.qlink.buffer.getFirstVehicleInBuffer());
		f.qlink.removeVehicle(vehicle2);
		f.qlink.doSimStep(202); // vehicle1 should not show up in buffer
		Assert.assertEquals(vehicle1, f.qlink.buffer.removeFirstVehicleInBuffer());
		Assert.assertNull(f.qlink.buffer.getFirstVehicleInBuffer());
		f.qlink.doSimStep(310);
		Assert.assertNull(f.qlink.buffer.getFirstVehicleInBuffer());
	}

	@Test
	public void testRemoveVehicle_fromBuffer() {
		Fixture f = new Fixture();

		Id veh1Id = new IdImpl(11);
		SimVehicle vehicle1 = new DefaultSimVehicle(new VehicleImpl(veh1Id, new VehicleTypeImpl(new IdImpl("1980"))));

		f.engine.setCurrentTime(200);
		f.qlink.addVehicle(vehicle1);
		f.qlink.doSimStep(250);
		f.qlink.doSimStep(300); // vehicle1 should now show up in buffer
		Assert.assertEquals(vehicle1, f.qlink.buffer.getFirstVehicleInBuffer());
		f.qlink.removeVehicle(vehicle1);
		Assert.assertNull(f.qlink.buffer.getFirstVehicleInBuffer());
	}

	@Test
	public void test_FreeflowSpeed_DrivingSingleVehicle() {
		Fixture f = new Fixture();

		Id veh1Id = new IdImpl(11);
		SimVehicle vehicle1 = new DefaultSimVehicle(new VehicleImpl(veh1Id, new VehicleTypeImpl(new IdImpl("1980"))));

		f.engine.setCurrentTime(200);
		f.qlink.addVehicle(vehicle1);
		Assert.assertNull(f.qlink.buffer.getFirstVehicleInBuffer());
		f.qlink.doSimStep(201);
		Assert.assertNull(f.qlink.buffer.getFirstVehicleInBuffer());
		f.qlink.doSimStep(202);
		Assert.assertNull(f.qlink.buffer.getFirstVehicleInBuffer());
		f.qlink.doSimStep(250);
		Assert.assertNull(f.qlink.buffer.getFirstVehicleInBuffer());
		f.qlink.doSimStep(299);
		Assert.assertNull(f.qlink.buffer.getFirstVehicleInBuffer());
		f.qlink.doSimStep(300);
		Assert.assertEquals(vehicle1, f.qlink.buffer.getFirstVehicleInBuffer());
		f.qlink.doSimStep(301);
		Assert.assertEquals(vehicle1, f.qlink.buffer.getFirstVehicleInBuffer());
	}

	@Test
	public void testParkVehicle() {
		Fixture f = new Fixture();

		Id veh1Id = new IdImpl(11);
		SimVehicle vehicle1 = new DefaultSimVehicle(new VehicleImpl(veh1Id, new VehicleTypeImpl(new IdImpl("1980"))));
		Id veh2Id = new IdImpl(5);
		SimVehicle vehicle2 = new DefaultSimVehicle(new VehicleImpl(veh2Id, new VehicleTypeImpl(new IdImpl("1979"))));

		f.qlink.addVehicle(vehicle1);
		f.qlink.parkVehicle(vehicle1);
		Assert.assertEquals(vehicle1, f.qlink.getParkedVehicle(veh1Id));
		f.qlink.parkVehicle(vehicle2); // veh2 is not on link!
		Assert.assertNull(f.qlink.getParkedVehicle(veh2Id));
	}

	private static class Fixture {

		/*package*/ final NetworkLayer net;
		/*package*/ final Link link;
		/*package*/ final FakeSimEngine engine;
		/*package*/ final QueueNetwork qnet;
		/*package*/ final QueueLink qlink;

		/*package*/ Fixture() {
			this.net = new NetworkLayer();
			this.net.addNode(this.net.getFactory().createNode(new IdImpl(1), new CoordImpl(0, 0)));
			this.net.addNode(this.net.getFactory().createNode(new IdImpl(2), new CoordImpl(1000, 0)));
			this.link = this.net.getFactory().createLink(new IdImpl(5), new IdImpl(1), new IdImpl(2));
			this.link.setLength(1000);
			this.link.setFreespeed(10);
			this.link.setCapacity(3600.0);
			this.link.setNumberOfLanes(1.0);
			this.engine = new FakeSimEngine();
			this.qnet = new QueueNetwork(this.engine, new Random());
			this.qlink = new QueueLink(this.link, this.qnet);
		}
	}
}
