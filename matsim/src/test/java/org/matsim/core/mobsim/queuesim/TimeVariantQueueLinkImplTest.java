/* *********************************************************************** *
 * project: org.matsim.*
 * TimeVariantLinkImplTest.java
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

package org.matsim.core.mobsim.queuesim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.TimeVariantLinkFactory;
import org.matsim.core.network.TimeVariantLinkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 * @author laemmel
 */
public class TimeVariantQueueLinkImplTest extends MatsimTestCase {

	//////////////////////////////////////////////////////////////////////
	// Time variant tests on QueueLink
	//////////////////////////////////////////////////////////////////////

	/**
	 * Tests the change of storage capacity if a lanes change event occurs
	 */
	public void testStorageCapacity() {
		Scenario s = new ScenarioImpl();
		s.getConfig().setQSimConfigGroup(new QSimConfigGroup());

		// create a network
		final NetworkLayer network = new NetworkLayer();
		NetworkFactoryImpl nf = new NetworkFactoryImpl(network);
		nf.setLinkFactory(new TimeVariantLinkFactory());
		network.setFactory(nf);
		network.setCapacityPeriod(3600.0);

		// the network has 2 nodes and 1 link, the length is 75 and has by default 4 lanes with a lanes with a cell size of 7.5 m --> storage cap = 40
		Node node1 = network.createAndAddNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl("2"), new CoordImpl(100, 0));
		TimeVariantLinkImpl link = (TimeVariantLinkImpl)network.createAndAddLink(new IdImpl("1"), node1, node2, 75, 10, 3600, 4);
		// add a lanes change to 2 at 8am.
		NetworkChangeEvent change1 = new NetworkChangeEvent(8*3600.0);
		change1.addLink(link);
		change1.setLanesChange(new ChangeValue(ChangeType.ABSOLUTE, 2));
		network.addNetworkChangeEvent(change1);
		// scale lanes by 0.5 at 10am.
		NetworkChangeEvent change2 = new NetworkChangeEvent(10*3600.0);
		change2.addLink(link);
		change2.setLanesChange(new ChangeValue(ChangeType.FACTOR, 0.5));
		network.addNetworkChangeEvent(change2);

		QueueNetwork qNetwork = new QueueNetwork(network, new QSim(s, null));
		QueueLink qLink = qNetwork.getQueueLink(new IdImpl("1"));
		qLink.finishInit();

		// the qLink has to be triggered if a network change event occurs - usually this is handled by the qsim
		// default
		assertEquals(40.,qLink.getStorageCapacity(),EPSILON);
		// tests if storage cap is still O.K. if the qlink is triggered for another reason before 8am
		qLink.recalcTimeVariantAttributes(7*3600.0);
		assertEquals(40.,qLink.getStorageCapacity(),EPSILON);

		// 8am
		qLink.recalcTimeVariantAttributes(8*3600.0);
		assertEquals(20.,qLink.getStorageCapacity(),EPSILON);
		// 10am
		qLink.recalcTimeVariantAttributes(10*3600.0);
		assertEquals(10.,qLink.getStorageCapacity(),EPSILON);
		//it's also possible to move backward in time ...
		qLink.recalcTimeVariantAttributes(7*3600.0);
		assertEquals(40., qLink.getStorageCapacity(), EPSILON);
	}

	/**
	 * Tests the change of flow capacity
	 */
	public void testFlowCapacity(){
		Scenario s = new ScenarioImpl();
		s.getConfig().setQSimConfigGroup(new QSimConfigGroup());

		// create a network
		final NetworkLayer network = new NetworkLayer();
		NetworkFactoryImpl nf = new NetworkFactoryImpl(network);
		nf.setLinkFactory(new TimeVariantLinkFactory());
		network.setFactory(nf);
		network.setCapacityPeriod(3600.0);

		// the network has 2 nodes and 1 link, the length is 75 and has  a flow capacity of 1 Veh/s by default
		Node node1 = network.createAndAddNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl("2"), new CoordImpl(100, 0));
		Link link = network.createAndAddLink(new IdImpl("1"), node1, node2, 75, 10, 3600, 4);
		// add a flow capacity change to 2 Veh/s at 8am.
		NetworkChangeEvent change1 = new NetworkChangeEvent(8*3600.0);
		change1.addLink(link);
		change1.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE, 2));
		network.addNetworkChangeEvent(change1);
		// add a flow capacity change factor of 0.5 at 10am.
		NetworkChangeEvent change2 = new NetworkChangeEvent(10*3600.0);
		change2.addLink(link);
		change2.setFlowCapacityChange(new ChangeValue(ChangeType.FACTOR, 0.5));
		network.addNetworkChangeEvent(change2);

		QueueNetwork qNetwork = new QueueNetwork(network, new QSim(s, null));
		QueueLink qLink = qNetwork.getQueueLink(new IdImpl("1"));
		qLink.finishInit();

		//default
		assertEquals(1., qLink.getSimulatedFlowCapacity(), EPSILON);
		// 7am
		qLink.recalcTimeVariantAttributes(7*3600.0);
		assertEquals(1., qLink.getSimulatedFlowCapacity(), EPSILON);
		// 8am
		qLink.recalcTimeVariantAttributes(8*3600.0);
		assertEquals(2., qLink.getSimulatedFlowCapacity(), EPSILON);
		// 10am
		qLink.recalcTimeVariantAttributes(10*3600.0);
		assertEquals(1., qLink.getSimulatedFlowCapacity(), EPSILON);
		//it's also possible to move backward in time ...
		qLink.recalcTimeVariantAttributes(8*3600.0);
		assertEquals(2., qLink.getSimulatedFlowCapacity(), EPSILON);

	}
}
