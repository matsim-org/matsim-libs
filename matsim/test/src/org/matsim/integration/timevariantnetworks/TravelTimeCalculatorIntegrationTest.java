/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCalculatorIntegrationTest.java
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

package org.matsim.integration.timevariantnetworks;

import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.network.NetworkChangeEvent;
import org.matsim.network.NetworkFactory;
import org.matsim.network.NetworkLayer;
import org.matsim.network.TimeVariantLinkImpl;
import org.matsim.network.NetworkChangeEvent.ChangeType;
import org.matsim.network.NetworkChangeEvent.ChangeValue;
import org.matsim.router.util.TravelTime;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.trafficmonitoring.TravelTimeCalculator;
import org.matsim.trafficmonitoring.TravelTimeAggregatorFactory;
import org.matsim.trafficmonitoring.TravelTimeDataHashMap;
import org.matsim.utils.geometry.CoordImpl;

public class TravelTimeCalculatorIntegrationTest extends MatsimTestCase {

	public void testTravelTimeCalculatorArray() {
		loadConfig(null);

		// create a network
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);
		network.setCapacityPeriod(3600.0);

		// the netework has 4 nodes and 3 links, each link by default 100 long and freespeed = 10 --> freespeed travel time = 10.0
		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(100, 0));
		Node node3 = network.createNode(new IdImpl("3"), new CoordImpl(200, 0));
		Node node4 = network.createNode(new IdImpl("4"), new CoordImpl(300, 0));
		Link link1 = network.createLink(new IdImpl("1"), node1, node2, 100, 10, 3600, 1);
		TimeVariantLinkImpl link2 = (TimeVariantLinkImpl)network.createLink(new IdImpl("2"), node2, node3, 100, 10, 3600, 1);
		network.createLink(new IdImpl("3"), node3, node4, 100, 10, 3600, 1);

		// add a freespeed change to 20 at 8am.
		NetworkChangeEvent change = new NetworkChangeEvent(8*3600.0);
		change.addLink(link2);
		change.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, 20));
		network.addNetworkChangeEvent(change);

		// create a travel time calculator object
		TravelTime ttcalc = new TravelTimeCalculator(network);

		// do the tests
		assertEquals(10.0, ttcalc.getLinkTravelTime(link2, 7*3600.0), EPSILON);
		assertEquals(5.0, ttcalc.getLinkTravelTime(link2, 8*3600.0), EPSILON);
		assertEquals(10.0, ttcalc.getLinkTravelTime(link1, 7*3600.0), EPSILON);
		assertEquals(10.0, ttcalc.getLinkTravelTime(link1, 8*3600.0), EPSILON);
	}

	public void testTravelTimeCalculatorHashMap() {
		loadConfig(null);

		// create a network
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);
		network.setCapacityPeriod(3600.0);

		// the netework has 4 nodes and 3 links, each link by default 100 long and freespeed = 10 --> freespeed travel time = 10.0
		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(100, 0));
		Node node3 = network.createNode(new IdImpl("3"), new CoordImpl(200, 0));
		Node node4 = network.createNode(new IdImpl("4"), new CoordImpl(300, 0));
		Link link1 = network.createLink(new IdImpl("1"), node1, node2, 100, 10, 3600, 1);
		TimeVariantLinkImpl link2 = (TimeVariantLinkImpl)network.createLink(new IdImpl("2"), node2, node3, 100, 10, 3600, 1);
		network.createLink(new IdImpl("3"), node3, node4, 100, 10, 3600, 1);

		// add a freespeed change to 20 at 8am.
		NetworkChangeEvent change = new NetworkChangeEvent(8*3600.0);
		change.addLink(link2);
		change.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, 20));
		network.addNetworkChangeEvent(change);

		// create a travel time calculator object
		TravelTimeAggregatorFactory factory = new TravelTimeAggregatorFactory();
		factory.setTravelTimeDataPrototype(TravelTimeDataHashMap.class);
		
		TravelTime ttcalc = new TravelTimeCalculator(network, 15*60, 30*3600, factory);

		// do the tests
		assertEquals(10.0, ttcalc.getLinkTravelTime(link2, 7*3600.0), EPSILON);
		assertEquals(5.0, ttcalc.getLinkTravelTime(link2, 8*3600.0), EPSILON);
		assertEquals(10.0, ttcalc.getLinkTravelTime(link1, 7*3600.0), EPSILON);
		assertEquals(10.0, ttcalc.getLinkTravelTime(link1, 8*3600.0), EPSILON);
	}

}
