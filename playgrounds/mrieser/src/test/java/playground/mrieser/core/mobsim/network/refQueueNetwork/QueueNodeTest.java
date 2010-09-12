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

package playground.mrieser.core.mobsim.network.refQueueNetwork;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.mrieser.core.mobsim.api.DriverAgent;
import playground.mrieser.core.mobsim.api.SimVehicle;
import playground.mrieser.core.mobsim.fakes.FakeSimEngine;
import playground.mrieser.core.mobsim.fakes.FakeSimVehicle;
import playground.mrieser.core.mobsim.network.api.SimLink;
import playground.mrieser.core.mobsim.network.refQueueNetwork.QueueLink;
import playground.mrieser.core.mobsim.network.refQueueNetwork.QueueNetwork;
import playground.mrieser.core.mobsim.network.refQueueNetwork.QueueNode;

/**
 * @author mrieser
 */
public class QueueNodeTest {

	@Test
	public void testMoveNode_singleCarAwaiting() {
		Fixture f = new Fixture();

		SimVehicle veh1 = new FakeSimVehicle(new IdImpl(5));
		TestDriverAgent driver = new TestDriverAgent();
		veh1.setDriver(driver);

		f.qlink1.buffer.updateCapacity();
		f.qlink1.buffer.addVehicle(veh1, 100);
		Assert.assertEquals(0, driver.count);
		f.qnode.moveNode(100);
		Assert.assertEquals(1, driver.count);
		f.qlink3.parkVehicle(veh1);
		Assert.assertEquals(veh1, f.qlink3.getParkedVehicle(veh1.getId()));
	}

	@Test
	public void testMoveNode_multipleCarsOnOneInlink() {
		Fixture f = new Fixture();

		SimVehicle veh1 = new FakeSimVehicle(new IdImpl(5));
		TestDriverAgent driver1 = new TestDriverAgent();
		veh1.setDriver(driver1);
		SimVehicle veh2 = new FakeSimVehicle(new IdImpl(10));
		TestDriverAgent driver2 = new TestDriverAgent();
		veh2.setDriver(driver2);

		f.qlink1.buffer.setFlowCapacity(2.0);
		f.qlink1.buffer.updateCapacity();
		f.qlink1.buffer.addVehicle(veh1, 100);
		f.qlink1.buffer.addVehicle(veh2, 100);
		Assert.assertEquals(0, driver1.count);
		Assert.assertEquals(0, driver2.count);
		f.qnode.moveNode(100);
		Assert.assertEquals(1, driver1.count);
		Assert.assertEquals(1, driver2.count);
		f.qlink3.parkVehicle(veh1);
		f.qlink3.parkVehicle(veh2);
		Assert.assertEquals(veh1, f.qlink3.getParkedVehicle(veh1.getId()));
		Assert.assertEquals(veh2, f.qlink3.getParkedVehicle(veh2.getId()));
	}

	@Test
	public void test_Deadlock() {
		Fixture f = new Fixture();

		SimVehicle veh1 = new FakeSimVehicle(new IdImpl(5));
		TestDriverAgent driver1 = new TestDriverAgent();
		veh1.setDriver(driver1);

		while (f.qlink3.hasSpace()) {
			f.qlink3.addVehicleFromIntersection(new FakeSimVehicle(new IdImpl("foo")));
		}

		f.qlink1.buffer.setFlowCapacity(1.0);
		f.qlink1.buffer.updateCapacity();
		f.qlink1.buffer.addVehicle(veh1, 0);
		Assert.assertEquals(0, driver1.count);
		Assert.assertFalse(f.qlink1.buffer.hasSpace());
		f.qlink1.buffer.updateCapacity();
		f.qnode.moveNode(10);
		Assert.assertEquals(0, driver1.count);
		f.qlink1.buffer.updateCapacity();
		f.qnode.moveNode(50);
		Assert.assertEquals(0, driver1.count);
		f.qlink1.buffer.updateCapacity();
		f.qnode.moveNode(99);
		Assert.assertEquals(0, driver1.count);
		Assert.assertFalse(f.qlink1.buffer.hasSpace());
		f.qlink1.buffer.updateCapacity();
		f.qnode.moveNode(100); // not moved to next link
		Assert.assertEquals(0, driver1.count);
		Assert.assertFalse(f.qlink1.buffer.hasSpace());
		f.qlink1.buffer.updateCapacity();
		f.qnode.moveNode(101);
		Assert.assertEquals(0, driver1.count); // not moved to next link
		f.qlink1.buffer.updateCapacity();
		Assert.assertTrue(f.qlink1.buffer.hasSpace()); // but no longer in buffer
		f.qlink3.parkVehicle(veh1); // should not be able to park, as it is not there
		Assert.assertNull(f.qlink3.getParkedVehicle(veh1.getId())); // so we should get null back
	}

	@Test
	public void test_Deadlock_nonDefaultStuckTime() {
		Fixture f = new Fixture();

		f.qnet.setStuckTime(20);

		SimVehicle veh1 = new FakeSimVehicle(new IdImpl(5));
		TestDriverAgent driver1 = new TestDriverAgent();
		veh1.setDriver(driver1);

		while (f.qlink3.hasSpace()) {
			f.qlink3.addVehicleFromIntersection(new FakeSimVehicle(new IdImpl("foo")));
		}

		f.qlink1.buffer.setFlowCapacity(1.0);
		f.qlink1.buffer.updateCapacity();
		f.qlink1.buffer.addVehicle(veh1, 0);
		Assert.assertEquals(0, driver1.count);
		Assert.assertFalse(f.qlink1.buffer.hasSpace());
		f.qlink1.buffer.updateCapacity();
		f.qnode.moveNode(10);
		Assert.assertEquals(0, driver1.count);
		f.qlink1.buffer.updateCapacity();
		f.qnode.moveNode(5);
		Assert.assertEquals(0, driver1.count);
		f.qlink1.buffer.updateCapacity();
		f.qnode.moveNode(19);
		Assert.assertEquals(0, driver1.count);
		Assert.assertFalse(f.qlink1.buffer.hasSpace());
		f.qlink1.buffer.updateCapacity();
		f.qnode.moveNode(20); // not moved to next link
		Assert.assertEquals(0, driver1.count);
		Assert.assertFalse(f.qlink1.buffer.hasSpace());
		f.qlink1.buffer.updateCapacity();
		f.qnode.moveNode(21);
		Assert.assertEquals(0, driver1.count); // not moved to next link
		f.qlink1.buffer.updateCapacity();
		Assert.assertTrue(f.qlink1.buffer.hasSpace()); // but no longer in buffer
		f.qlink3.parkVehicle(veh1); // should not be able to park, as it is not there
		Assert.assertNull(f.qlink3.getParkedVehicle(veh1.getId())); // so we should get null back
	}

	private static class Fixture {

		/*package*/ final NetworkLayer net;
		/*package*/ final Link link1;
		/*package*/ final Link link2;
		/*package*/ final Link link3;
		/*package*/ final FakeSimEngine engine;
		/*package*/ final QueueNetwork qnet;
		/*package*/ final QueueLink qlink1;
		/*package*/ final QueueLink qlink2;
		/*package*/ final QueueLink qlink3;
		/*package*/ final QueueNode qnode;

		/*package*/ Fixture() {
			Node node;
			this.net = new NetworkLayer();
			this.net.addNode(this.net.getFactory().createNode(new IdImpl(1), new CoordImpl(0, 0)));
			this.net.addNode(this.net.getFactory().createNode(new IdImpl(2), new CoordImpl(0, 10000)));
			this.net.addNode(node = this.net.getFactory().createNode(new IdImpl(3), new CoordImpl(500, 500)));
			this.net.addNode(this.net.getFactory().createNode(new IdImpl(4), new CoordImpl(1500, 500)));
			this.link1 = this.net.getFactory().createLink(new IdImpl(1), new IdImpl(1), new IdImpl(3));
			this.link1.setLength(1000);
			this.link1.setFreespeed(10);
			this.link1.setCapacity(3600.0);
			this.link1.setNumberOfLanes(1.0);
			this.link2 = this.net.getFactory().createLink(new IdImpl(2), new IdImpl(2), new IdImpl(3));
			this.link2.setLength(1000);
			this.link2.setFreespeed(10);
			this.link2.setCapacity(3600.0);
			this.link2.setNumberOfLanes(1.0);
			this.link3 = this.net.getFactory().createLink(new IdImpl(3), new IdImpl(3), new IdImpl(4));
			this.link3.setLength(1000);
			this.link3.setFreespeed(10);
			this.link3.setCapacity(3600.0);
			this.link3.setNumberOfLanes(1.0);
			this.net.addLink(this.link1);
			this.net.addLink(this.link2);
			this.net.addLink(this.link3);
			this.engine = new FakeSimEngine();
			this.qnet = new QueueNetwork(this.engine);
			this.qlink1 = new QueueLink(this.link1, this.qnet);
			this.qlink2 = new QueueLink(this.link2, this.qnet);
			this.qlink3 = new QueueLink(this.link3, this.qnet);
			this.qnet.addLink(this.qlink1);
			this.qnet.addLink(this.qlink2);
			this.qnet.addLink(this.qlink3);
			this.qnode = new QueueNode(node, this.qnet, new Random(1));
		}
	}

	/*package*/ static class TestDriverAgent implements DriverAgent {
		/*package*/ int count = 0;
		@Override
		public Id getNextLinkId() {
			return new IdImpl(3);
		}
		@Override
		public void notifyMoveToNextLink() {
			this.count++;
		}
		@Override
		public double getNextActionOnCurrentLink() {
			return -1.0;
		}
		@Override
		public void handleNextAction(final SimLink link) {
		}
	}

}
