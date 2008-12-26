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

package org.matsim.network;

import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.network.NetworkChangeEvent.ChangeType;
import org.matsim.network.NetworkChangeEvent.ChangeValue;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.misc.Time;

/**
 * @author mrieser
 * @author laemmel
 */
public class TimeVariantLinkImplTest extends MatsimTestCase {

	/** Tests the method {@link TimeVariantLinkImpl#getFreespeedTravelTime(double)}.	 */
	public void testGetFreespeedTravelTime(){
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);
		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(0, 1000));
		Node node3 = network.createNode(new IdImpl("3"), new CoordImpl(1000, 2000));
		Node node4 = network.createNode(new IdImpl("4"), new CoordImpl(2000, 2000));
		final Link link1 = network.createLink(new IdImpl("1"), node1, node2, 1000, 1.667, 3600, 1);
		final Link link3 = network.createLink(new IdImpl("3"), node3, node4, 1000, 1.667, 3600, 1);

		final double [] queryDates = {org.matsim.utils.misc.Time.UNDEFINED_TIME, 0., 1., 2., 3., 4.};

		// link1 change event absolute, undef. endtime
		final double [] responsesLink1 = {1.667, 1.667, 10., 10., 10., 10.};
		NetworkChangeEvent event = new NetworkChangeEvent(1);
		event.addLink(link1);
		event.setFreespeedChange(new ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE,10.));
		((TimeVariantLinkImpl)link1).applyEvent(event);

		// link3 change event factor, undef. endtime
		final double [] responsesLink3 = {1.667, 1.667, 10.002, 10.002, 10.002, 10.002};
		event = new NetworkChangeEvent(1);
		event.addLink(link3);
		event.setFreespeedChange(new ChangeValue(NetworkChangeEvent.ChangeType.FACTOR,6.));
		((TimeVariantLinkImpl)link3).applyEvent(event);

		for (int i = 0; i < queryDates.length; i++) {
			assertEquals(responsesLink1[i], link1.getFreespeed(queryDates[i]), EPSILON);
			assertEquals(responsesLink3[i], link3.getFreespeed(queryDates[i]), EPSILON);
		}
	}

	/**
	 * Tests whether an absolute change in the freespeed really can be seen in the link's travel time
	 */
	public void testFreespeedChangeAbsolute() {
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);

		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(100, 0));
		TimeVariantLinkImpl link = (TimeVariantLinkImpl)network.createLink(new IdImpl("1"), node1, node2, 100, 10, 3600, 1);

		// test base values
		assertEquals(10.0, link.getFreespeed(Time.UNDEFINED_TIME), EPSILON);
		assertEquals(10.0, link.getFreespeedTravelTime(Time.UNDEFINED_TIME), EPSILON);

		// add an absolute change
		NetworkChangeEvent change = new NetworkChangeEvent(7*3600.0);
		change.addLink(link);
		change.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, 20));
		link.applyEvent(change);

		// do the tests
		assertEquals(10.0, link.getFreespeed(Time.UNDEFINED_TIME), EPSILON); // at undefined time, return base value
		assertEquals(10.0, link.getFreespeed(7*3600.0 - 1.0), EPSILON);  // one second before change, still base value
		assertEquals(10.0, link.getFreespeed(7*3600.0 - 0.1), EPSILON);  // just before change, still base value
		assertEquals(20.0, link.getFreespeed(7*3600.0), EPSILON); // just on time of change, new value
		assertEquals(20.0, link.getFreespeed(8*3600.0), EPSILON); // some time later, still new value

		// test derived values
		assertEquals(10.0, link.getFreespeedTravelTime(Time.UNDEFINED_TIME), EPSILON); // and now the same tests for the travel time
		assertEquals(10.0, link.getFreespeedTravelTime(7*3600.0 - 1.0), EPSILON);
		assertEquals(10.0, link.getFreespeedTravelTime(7*3600.0 - 0.1), EPSILON);
		assertEquals(5.0, link.getFreespeedTravelTime(7*3600.0), EPSILON);
		assertEquals(5.0, link.getFreespeedTravelTime(8*3600.0), EPSILON);
		assertEquals(5.0, link.getFreespeedTravelTime(24*3600.0), EPSILON); // also test if it "wraps around" on 24 hours, it shouldn't
		assertEquals(5.0, link.getFreespeedTravelTime(30*3600.0), EPSILON);
		assertEquals(5.0, link.getFreespeedTravelTime(36*3600.0), EPSILON);
	}

	/**
	 * Tests whether a relative change in the freespeed really can be seen in the link's travel time
	 */
	public void testFreespeedChangeRelative() {
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);

		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(100, 0));
		TimeVariantLinkImpl link = (TimeVariantLinkImpl)network.createLink(new IdImpl("1"), node1, node2, 100, 10, 3600, 1);

		// test base values
		assertEquals(10.0, link.getFreespeed(Time.UNDEFINED_TIME), EPSILON);
		assertEquals(10.0, link.getFreespeedTravelTime(Time.UNDEFINED_TIME), EPSILON);

		// add a relative change
		NetworkChangeEvent change = new NetworkChangeEvent(7*3600.0);
		change.addLink(link);
		change.setFreespeedChange(new ChangeValue(ChangeType.FACTOR, 0.5));
		link.applyEvent(change);

		// do the tests for the actual value
		assertEquals(10.0, link.getFreespeed(Time.UNDEFINED_TIME), EPSILON); // at undefined time, return base value
		assertEquals(10.0, link.getFreespeed(7*3600.0 - 1.0), EPSILON);  // one second before change, still base value
		assertEquals(10.0, link.getFreespeed(7*3600.0 - 0.1), EPSILON);  // just before change, still base value
		assertEquals(5.0, link.getFreespeed(7*3600.0), EPSILON); // just on time of change, new value
		assertEquals(5.0, link.getFreespeed(8*3600.0), EPSILON); // some time later, still new value

		// do tests for derived values
		assertEquals(10.0, link.getFreespeedTravelTime(Time.UNDEFINED_TIME), EPSILON); // and now the same tests for the travel time
		assertEquals(10.0, link.getFreespeedTravelTime(7*3600.0 - 1.0), EPSILON);
		assertEquals(10.0, link.getFreespeedTravelTime(7*3600.0 - 0.1), EPSILON);
		assertEquals(20.0, link.getFreespeedTravelTime(7*3600.0), EPSILON);
		assertEquals(20.0, link.getFreespeedTravelTime(8*3600.0), EPSILON);
		assertEquals(20.0, link.getFreespeedTravelTime(24*3600.0), EPSILON); // also test if it "wraps around" on 24 hours, it shouldn't
		assertEquals(20.0, link.getFreespeedTravelTime(30*3600.0), EPSILON);
		assertEquals(20.0, link.getFreespeedTravelTime(36*3600.0), EPSILON);
	}

	/**
	 * Tests how multiple freespeed changes interact with each other on the link.
	 */
	public void testMultipleFreespeedChanges() {
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);

		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(100, 0));
		TimeVariantLinkImpl link = (TimeVariantLinkImpl)network.createLink(new IdImpl("1"), node1, node2, 100, 10, 3600, 1);

		// test base values
		assertEquals(10.0, link.getFreespeed(Time.UNDEFINED_TIME), EPSILON);
		assertEquals(10.0, link.getFreespeedTravelTime(Time.UNDEFINED_TIME), EPSILON);

		// add some changes:
		// - first a change event starting at 7am
		NetworkChangeEvent change = new NetworkChangeEvent(7*3600.0);
		change.addLink(link);
		change.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, 20));
		link.applyEvent(change);
		// - second, a change event starting at from 8am
		NetworkChangeEvent change2 = new NetworkChangeEvent(8*3600.0);
		change2.addLink(link);
		change2.setFreespeedChange(new ChangeValue(ChangeType.FACTOR, 3.0));
		link.applyEvent(change2);
		// - third a change event starting at 10am
		NetworkChangeEvent change3 = new NetworkChangeEvent(10*3600.0);
		change3.addLink(link);
		change3.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, 30));
		link.applyEvent(change3);

		/* I would now expect the following speeds:
		 * 0am-7am: 10
		 * 7am-8am: 20
		 * 8am-10am: 60
		 * 10am and later: 30
		 */

		// do the tests for the actual value
		assertEquals(10.0, link.getFreespeed(Time.UNDEFINED_TIME), EPSILON); // at undefined time, return base value
		assertEquals(10.0, link.getFreespeed(7*3600.0 - 1.0), EPSILON);
		assertEquals(20.0, link.getFreespeed(7*3600.0), EPSILON);
		assertEquals(20.0, link.getFreespeed(8*3600.0-1), EPSILON);
		assertEquals(60.0, link.getFreespeed(8*3600.0), EPSILON);
		assertEquals(60.0, link.getFreespeed(10*3600.0-1), EPSILON);
		assertEquals(30.0, link.getFreespeed(10*3600.0), EPSILON);
		assertEquals(30.0, link.getFreespeed(18*3600.0), EPSILON);

		// everything fine so long, now add some more changes in a chronological arbitrary order

		// - a change event starting 12am
		NetworkChangeEvent change4 = new NetworkChangeEvent(12*3600.0);
		change4.addLink(link);
		change4.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, 40));
		link.applyEvent(change4);
		// - a change event starting 11am
		NetworkChangeEvent change5 = new NetworkChangeEvent(11*3600.0);
		change5.addLink(link);
		change5.setFreespeedChange(new ChangeValue(ChangeType.FACTOR, 0.5));
		link.applyEvent(change5);
		// - a change event starting at 9am
		NetworkChangeEvent change6 = new NetworkChangeEvent(9*3600.0);
		change6.addLink(link);
		change6.setFreespeedChange(new ChangeValue(ChangeType.FACTOR, 0.5));
		link.applyEvent(change6);

		/* I would now expect the following speeds
		 * 0am-7am: 10
		 * 7am-8am: 20
		 * 8am-9am: 60
		 * 9am-10am: 30
		 * 10am-11am: 30
		 * 11am-12am: 15
		 * 12am and later: 40
		 *
		 */

		// test again
		assertEquals(10.0, link.getFreespeed(6*3600.0), EPSILON);
		assertEquals(20.0, link.getFreespeed(7*3600.0), EPSILON);
		assertEquals(60.0, link.getFreespeed(8*3600.0), EPSILON);
		assertEquals(60.0, link.getFreespeed(9*3600.0-1), EPSILON);
		assertEquals(30.0, link.getFreespeed(9*3600.0), EPSILON);
		assertEquals(30.0, link.getFreespeed(10*3600.0-1), EPSILON);
		assertEquals(30.0, link.getFreespeed(10*3600.0), EPSILON);
		assertEquals(30.0, link.getFreespeed(11*3600.0-1), EPSILON);
		assertEquals(15.0, link.getFreespeed(11*3600.0), EPSILON);
		assertEquals(15.0, link.getFreespeed(12*3600.0-1), EPSILON);
		assertEquals(40.0, link.getFreespeed(12*3600.0), EPSILON);
		assertEquals(40.0, link.getFreespeed(18*3600.0), EPSILON);
	}

	/**
	 * Tests whether an absolute change to the flow capacity really can be observed on the link .
	 */
	public void testFlowCapChangeAbsolute() {
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);
		network.setCapacityPeriod(3600.0);

		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(100, 0));
		TimeVariantLinkImpl link = (TimeVariantLinkImpl)network.createLink(new IdImpl("1"), node1, node2, 100, 10, 3600, 1);

		// test base values
		assertEquals(3600.0, link.getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME), EPSILON);
		assertEquals(1.0, link.getFlowCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME), EPSILON);

		// add an absolute change
		NetworkChangeEvent change = new NetworkChangeEvent(7*3600.0);
		change.addLink(link);
		change.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE, 2));
		link.applyEvent(change);

		// do the tests
		assertEquals(3600.0, link.getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME), EPSILON);
		assertEquals(1.0, link.getFlowCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME), EPSILON);
		assertEquals(2.0, link.getFlowCapacity(7*3600), EPSILON);

		// test derived values
		// TODO test flowcap by sending vehicles through the link, this requires to create a queuenetwork from this time-variant network
	}

	// TODO additional tests for flowCap once there are useful implementations

	/**
	 * Tests whether an absolute change to the number of lanes really can be observed on the link.
	 */
	public void testLanesChangeAbsolute() {
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);
		network.setCapacityPeriod(3600.0);

		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(100, 0));
		TimeVariantLinkImpl link = (TimeVariantLinkImpl)network.createLink(new IdImpl("1"), node1, node2, 100, 10, 3600, 1);

		// test base values
		assertEquals(1.0, link.getLanes(org.matsim.utils.misc.Time.UNDEFINED_TIME), EPSILON);

		// add an absolute change
		NetworkChangeEvent change = new NetworkChangeEvent(7*3600.0);
		change.addLink(link);
		change.setLanesChange(new ChangeValue(ChangeType.ABSOLUTE, 2.0));
		link.applyEvent(change);

		// do the tests
		assertEquals(1.0, link.getLanes(org.matsim.utils.misc.Time.UNDEFINED_TIME), EPSILON);
		assertEquals(2.0, link.getLanes(7*3600), EPSILON);

		// test derived values
		// TODO e.g. test storage capacity on a queuelink based on this time-variant link
	}



	//////////////////////////////////////////////////////////////////////
	// Time variant tests on QueueLink
	//////////////////////////////////////////////////////////////////////

	/**
	 * Tests the change of storage capacity if a lanes change event occurs
	 */
	public void testStorageCapacity() {
		loadConfig(null);

		// create a network
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);
		network.setCapacityPeriod(3600.0);
		Gbl.createWorld().setNetworkLayer(network);

		// the network has 2 nodes and 1 link, the length is 75 and has by default 4 lanes with a lanes with a cell size of 7.5 m --> storage cap = 40
		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(100, 0));
		TimeVariantLinkImpl link = (TimeVariantLinkImpl)network.createLink(new IdImpl("1"), node1, node2, 75, 10, 3600, 4);
		Gbl.getWorld().complete();
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


		QueueNetwork qNetwork = new QueueNetwork(network);
		QueueLink qLink = qNetwork.getQueueLink(new IdImpl("1"));
		qLink.finishInit();

		// the qLink has to be triggered if a network change event occurs - usually this is handled by the qsim
		// default
		assertEquals(40.,qLink.getSpaceCap(),EPSILON);
		// tests if storage cap is still O.K. if the qlink is triggered for another reason before 8am
		qLink.recalcTimeVariantAttributes(7*3600.0);
		assertEquals(40.,qLink.getSpaceCap(),EPSILON);

		// 8am
		qLink.recalcTimeVariantAttributes(8*3600.0);
		assertEquals(20.,qLink.getSpaceCap(),EPSILON);
		// 10am
		qLink.recalcTimeVariantAttributes(10*3600.0);
		assertEquals(10.,qLink.getSpaceCap(),EPSILON);
		//it's also possible to move backward in time ...
		qLink.recalcTimeVariantAttributes(7*3600.0);
		assertEquals(40., qLink.getSpaceCap(), EPSILON);
	}

	/**
	 * Tests the change of flow capacity
	 */
	public void testFlowCapacity(){
		loadConfig(null);

		// create a network
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);
		network.setCapacityPeriod(3600.0);
		Gbl.createWorld().setNetworkLayer(network);

		// the network has 2 nodes and 1 link, the length is 75 and has  a flow capacity of 1 Veh/s by default
		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(100, 0));
		Link link = network.createLink(new IdImpl("1"), node1, node2, 75, 10, 3600, 4);
		Gbl.getWorld().complete();
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

		QueueNetwork qNetwork = new QueueNetwork(network);
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
