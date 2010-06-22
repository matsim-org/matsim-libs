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
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.mrieser.core.sim.api.DriverAgent;
import playground.mrieser.core.sim.api.SimVehicle;
import playground.mrieser.core.sim.fakes.FakeSimEngine;
import playground.mrieser.core.sim.fakes.FakeSimVehicle;
import playground.mrieser.core.sim.network.api.SimLink;

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
		f.qnode.moveNode(100, new Random(1));
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
		f.qnode.moveNode(100, new Random(1));
		Assert.assertEquals(1, driver1.count);
		Assert.assertEquals(1, driver2.count);
		f.qlink3.parkVehicle(veh1);
		f.qlink3.parkVehicle(veh2);
		Assert.assertEquals(veh1, f.qlink3.getParkedVehicle(veh1.getId()));
		Assert.assertEquals(veh2, f.qlink3.getParkedVehicle(veh2.getId()));
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
			this.qnet = new QueueNetwork(this.engine, new Random());
			this.qlink1 = new QueueLink(this.link1, this.qnet);
			this.qlink2 = new QueueLink(this.link2, this.qnet);
			this.qlink3 = new QueueLink(this.link3, this.qnet);
			this.qnet.addLink(this.qlink1);
			this.qnet.addLink(this.qlink2);
			this.qnet.addLink(this.qlink3);
			this.qnode = new QueueNode(node, this.qnet);
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
