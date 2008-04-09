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

import org.matsim.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkFactory;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Person;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author dgrether
 *
 */
public class QueueLinkTest extends MatsimTestCase {

	private NetworkLayer network;

	private NetworkFactory factory;

	private Node node1;

	private Node node2;

	private Link link;

	private QueueNetworkLayer queueNetwork;

	private QueueLink qlink;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.network = new NetworkLayer();
		this.network.setCapacityPeriod(1.0);
		this.factory = new NetworkFactory(this.network);
		this.node1 = this.factory.newNode("1", "0", "0", null);
		this.node2 = this.factory.newNode("2", "1", "0", null);
		this.link = this.factory.newLink("1", this.node1, this.node2, "1", "1",
				"1", "1", null, null);
		this.network.addLink(this.link);
		super.loadConfig(null);
		this.queueNetwork = new QueueNetworkLayer(this.network);
		this.qlink = this.queueNetwork.getQueueLink(new Id("1"));
		this.qlink.finishInit();
	}

	public void testInit() {
		assertNotNull(this.qlink);
		assertEquals(1.0, this.qlink.getSimulatedFlowCapacity());
		assertEquals(1.0, this.qlink.getSpaceCap());
		// TODO dg[april2008] this assertions are not covering everything in
		// QueueLink's constructor.
		// Extend the tests by checking the methods initFlowCapacity and
		// recalcCapacity
		assertEquals(this.link, this.qlink.getLink());
		assertEquals(this.queueNetwork.getQueueNode(new Id("2")), this.qlink
				.getToQueueNode());
	}

	public void testChangeSimulatedFlowCapacity() {
		assertEquals(1.0, this.qlink.getSimulatedFlowCapacity());
		this.qlink.changeSimulatedFlowCapacity(2.0);
		assertEquals(2.0, this.qlink.getSimulatedFlowCapacity());
		this.qlink.changeSimulatedFlowCapacity(0.5);
		assertEquals(1.0, this.qlink.getSimulatedFlowCapacity());
	}

	public void testAdd() {
		Vehicle v = new Vehicle();

		Person p = new Person(new Id("1"), null, 0, null, null, null);
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

}
