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

package playground.mrieser.core.mobsim.features.fastQueueNetworkFeature;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

import playground.mrieser.core.mobsim.api.MobsimVehicle;
import playground.mrieser.core.mobsim.fakes.FakeSimEngine;
import playground.mrieser.core.mobsim.fakes.FakeSimVehicle;
import playground.mrieser.core.mobsim.impl.DefaultSimVehicle;

/**
 * @author mrieser
 */
public class QueueBufferTest {

	@Test
	public void testSpaceCapacity() {
		Fixture f = new Fixture();

		MobsimVehicle veh1 = new DefaultSimVehicle(null);
		MobsimVehicle veh2 = new DefaultSimVehicle(null);
		MobsimVehicle veh3 = new DefaultSimVehicle(null);

		QueueBuffer buffer = new QueueBuffer(f.qlink);
		buffer.init();
		buffer.setFlowCapacity(2.0);
		buffer.updateCapacity();
		Assert.assertTrue(buffer.hasSpace());
		buffer.addVehicle(veh1, 100);
		Assert.assertTrue(buffer.hasSpace());
		buffer.addVehicle(veh2, 100);
		Assert.assertFalse(buffer.hasSpace());
		buffer.updateCapacity();
		Assert.assertFalse(buffer.hasSpace());

		buffer = new QueueBuffer(f.qlink);
		buffer.init();
		buffer.setFlowCapacity(2.5); // at the beginning, 3 vehicles have place
		buffer.updateCapacity();
		Assert.assertTrue(buffer.hasSpace());
		buffer.addVehicle(veh1, 100);
		Assert.assertTrue(buffer.hasSpace());
		buffer.addVehicle(veh2, 100);
		Assert.assertTrue(buffer.hasSpace());
		buffer.addVehicle(veh3, 100);
		Assert.assertFalse(buffer.hasSpace());
		buffer.updateCapacity();
		Assert.assertFalse(buffer.hasSpace());
	}

	@Test
	public void testFlowCapacity() {
		Fixture f = new Fixture();

		VehicleType defaultVehicleType = new VehicleTypeImpl(new IdImpl("defaultVehicleType"));
		MobsimVehicle[] vehicles = new MobsimVehicle[8000];
		for (int i = 0; i < vehicles.length; i++) {
			vehicles[i] = new DefaultSimVehicle(new VehicleImpl(new IdImpl(i), defaultVehicleType));
		}

		doTestFlowCapacity(360, f, vehicles);
		doTestFlowCapacity(1800, f, vehicles);
		doTestFlowCapacity(2880, f, vehicles);
		doTestFlowCapacity(3600, f, vehicles);
		doTestFlowCapacity(7200, f, vehicles);
		doTestFlowCapacity(6329, f, vehicles);
	}

	private void doTestFlowCapacity(final int vehPerHour, final Fixture f, final MobsimVehicle[] vehicles) {
		QueueBuffer buffer = new QueueBuffer(f.qlink);
		buffer.init();
		buffer.setFlowCapacity(vehPerHour / 3600.0);
		int cntInsert = 0;
		int cntExtract = 0;

		for (int sec = 0; sec < 3600; sec++) {
			buffer.updateCapacity();
			while (buffer.hasSpace()) {
				buffer.addVehicle(vehicles[cntInsert++], sec);
			}
			while (buffer.getFirstVehicleInBuffer() != null) {
				Assert.assertEquals(vehicles[cntExtract++], buffer.removeFirstVehicleInBuffer());
			}
		}
		Assert.assertEquals(vehPerHour, cntExtract);

		// test with 1800 veh/h
		buffer = new QueueBuffer(f.qlink);
		buffer.init();
		buffer.setFlowCapacity(0.5);
		cntInsert = 0;
		cntExtract = 0;

		for (int sec = 0; sec < 3600; sec++) {
			buffer.updateCapacity();
			while (buffer.hasSpace()) {
				buffer.addVehicle(vehicles[cntInsert++], sec);
			}
			while (buffer.getFirstVehicleInBuffer() != null) {
				Assert.assertEquals(vehicles[cntExtract++], buffer.removeFirstVehicleInBuffer());
			}
		}
		Assert.assertEquals(1800, cntExtract);

		// test with 7200 veh/h
		buffer = new QueueBuffer(f.qlink);
		buffer.init();
		buffer.setFlowCapacity(2.0);
		cntInsert = 0;
		cntExtract = 0;

		for (int sec = 0; sec < 3600; sec++) {
			buffer.updateCapacity();
			while (buffer.hasSpace()) {
				buffer.addVehicle(vehicles[cntInsert++], sec);
			}
			while (buffer.getFirstVehicleInBuffer() != null) {
				Assert.assertEquals(vehicles[cntExtract++], buffer.removeFirstVehicleInBuffer());
			}
		}
		Assert.assertEquals(7200, cntExtract);

		// test with 2880 veh/h
		buffer = new QueueBuffer(f.qlink);
		buffer.init();
		buffer.setFlowCapacity(0.8);
		cntInsert = 0;
		cntExtract = 0;

		for (int sec = 0; sec < 3600; sec++) {
			buffer.updateCapacity();
			while (buffer.hasSpace()) {
				buffer.addVehicle(vehicles[cntInsert++], sec);
			}
			while (buffer.getFirstVehicleInBuffer() != null) {
				Assert.assertEquals(vehicles[cntExtract++], buffer.removeFirstVehicleInBuffer());
			}
		}
		Assert.assertEquals(2880, cntExtract);

	}

	@Test
	public void testGetLastMovedTime() {
		Fixture f = new Fixture();
		QueueBuffer buffer = new QueueBuffer(f.qlink);
		buffer.init();
		buffer.setFlowCapacity(2.0);
		buffer.updateCapacity();

		Assert.assertEquals("lastMovedTimes should be undefined before first doSimStep().", Time.UNDEFINED_TIME, buffer.getLastMovedTime(), MatsimTestUtils.EPSILON);
		f.engine.setCurrentTime(10.0);
		buffer.addVehicle(new FakeSimVehicle(new IdImpl(5)), 10.0);
		Assert.assertEquals("lastMovedTimes should represent enter-time.", 10.0, buffer.getLastMovedTime(), MatsimTestUtils.EPSILON);
		f.engine.setCurrentTime(20.0);
		buffer.addVehicle(new FakeSimVehicle(new IdImpl(11)), 20.0);
		Assert.assertEquals("lastMovedTimes should represent enter-time of first vehicle.", 10.0, buffer.getLastMovedTime(), MatsimTestUtils.EPSILON);
		f.engine.setCurrentTime(50.0);
		buffer.removeFirstVehicleInBuffer();
		Assert.assertEquals("lastMovedTimes should represent time first vehicle left.", 50.0, buffer.getLastMovedTime(), MatsimTestUtils.EPSILON);
		f.engine.setCurrentTime(60.0 + f.qnet.getStuckTime());
		buffer.removeFirstVehicleInBuffer();
		Assert.assertEquals("lastMovedTimes should represent time when last vehicle left.", f.engine.getCurrentTime(), buffer.getLastMovedTime(), MatsimTestUtils.EPSILON);
	}

	private static class Fixture {

		/*package*/ final NetworkImpl net;
		/*package*/ final Link link;
		/*package*/ final FakeSimEngine engine;
		/*package*/ final QueueNetwork qnet;
		/*package*/ final QueueLink qlink;
		/*package*/ final QueueNode qnode;
		private final Operator operator = new SingleCPUOperator();

		/*package*/ Fixture() {
			this.net = NetworkImpl.createNetwork();
			Node node2 = this.net.getFactory().createNode(new IdImpl(2), new CoordImpl(1000, 0));
			this.net.addNode(this.net.getFactory().createNode(new IdImpl(1), new CoordImpl(0, 0)));
			this.net.addNode(node2);
			this.link = this.net.getFactory().createLink(new IdImpl(5), new IdImpl(1), new IdImpl(2));
			this.link.setLength(1000);
			this.link.setFreespeed(10);
			this.link.setCapacity(3600.0);
			this.link.setNumberOfLanes(1.0);

			this.engine = new FakeSimEngine();
			this.qnet = new QueueNetwork(this.engine, this.operator);
			this.qlink = new QueueLink(this.link, this.qnet, this.operator);
			this.qnet.addLink(this.qlink);
			this.qnode = new QueueNode(node2, this.qnet, this.operator, new Random(1980));
			this.qnet.addNode(this.qnode);
			this.qlink.buffer.init();
		}
	}
}
