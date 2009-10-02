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

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.network.TimeVariantLinkFactory;
import org.matsim.core.network.TimeVariantLinkImpl;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeDataHashMapFactory;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

public class TravelTimeCalculatorIntegrationTest extends MatsimTestCase {

	public void testTravelTimeCalculatorArray() {
		Config config = loadConfig(null);

		// create a network
		NetworkFactoryImpl nf = new NetworkFactoryImpl();
		nf.setLinkFactory(new TimeVariantLinkFactory());
		final NetworkLayer network = new NetworkLayer(nf);
		network.setCapacityPeriod(3600.0);

		// the netework has 4 nodes and 3 links, each link by default 100 long and freespeed = 10 --> freespeed travel time = 10.0
		NodeImpl node1 = network.createAndAddNode(new IdImpl("1"), new CoordImpl(0, 0));
		NodeImpl node2 = network.createAndAddNode(new IdImpl("2"), new CoordImpl(100, 0));
		NodeImpl node3 = network.createAndAddNode(new IdImpl("3"), new CoordImpl(200, 0));
		NodeImpl node4 = network.createAndAddNode(new IdImpl("4"), new CoordImpl(300, 0));
		LinkImpl link1 = network.createAndAddLink(new IdImpl("1"), node1, node2, 100, 10, 3600, 1);
		TimeVariantLinkImpl link2 = (TimeVariantLinkImpl)network.createAndAddLink(new IdImpl("2"), node2, node3, 100, 10, 3600, 1);
		network.createAndAddLink(new IdImpl("3"), node3, node4, 100, 10, 3600, 1);

		// add a freespeed change to 20 at 8am.
		NetworkChangeEvent change = new NetworkChangeEvent(8*3600.0);
		change.addLink(link2);
		change.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, 20));
		network.addNetworkChangeEvent(change);

		// create a travel time calculator object
		TravelTime ttcalc = new TravelTimeCalculator(network,config.travelTimeCalculator());

		// do the tests
		assertEquals(10.0, ttcalc.getLinkTravelTime(link2, 7*3600.0), EPSILON);
		assertEquals(5.0, ttcalc.getLinkTravelTime(link2, 8*3600.0), EPSILON);
		assertEquals(10.0, ttcalc.getLinkTravelTime(link1, 7*3600.0), EPSILON);
		assertEquals(10.0, ttcalc.getLinkTravelTime(link1, 8*3600.0), EPSILON);
	}

	public void testTravelTimeCalculatorHashMap() {
		Config config = loadConfig(null);

		// create a network
		NetworkFactoryImpl nf = new NetworkFactoryImpl();
		nf.setLinkFactory(new TimeVariantLinkFactory());
		final NetworkLayer network = new NetworkLayer(nf);
		network.setCapacityPeriod(3600.0);

		// the netework has 4 nodes and 3 links, each link by default 100 long and freespeed = 10 --> freespeed travel time = 10.0
		NodeImpl node1 = network.createAndAddNode(new IdImpl("1"), new CoordImpl(0, 0));
		NodeImpl node2 = network.createAndAddNode(new IdImpl("2"), new CoordImpl(100, 0));
		NodeImpl node3 = network.createAndAddNode(new IdImpl("3"), new CoordImpl(200, 0));
		NodeImpl node4 = network.createAndAddNode(new IdImpl("4"), new CoordImpl(300, 0));
		LinkImpl link1 = network.createAndAddLink(new IdImpl("1"), node1, node2, 100, 10, 3600, 1);
		TimeVariantLinkImpl link2 = (TimeVariantLinkImpl)network.createAndAddLink(new IdImpl("2"), node2, node3, 100, 10, 3600, 1);
		network.createAndAddLink(new IdImpl("3"), node3, node4, 100, 10, 3600, 1);

		// add a freespeed change to 20 at 8am.
		NetworkChangeEvent change = new NetworkChangeEvent(8*3600.0);
		change.addLink(link2);
		change.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, 20));
		network.addNetworkChangeEvent(change);

		// create a travel time calculator object
		TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, 15*60, 30*3600, config.travelTimeCalculator());
		ttcalc.setTravelTimeDataFactory(new TravelTimeDataHashMapFactory(network));

		// do the tests
		assertEquals(10.0, ttcalc.getLinkTravelTime(link2, 7*3600.0), EPSILON);
		assertEquals(5.0, ttcalc.getLinkTravelTime(link2, 8*3600.0), EPSILON);
		assertEquals(10.0, ttcalc.getLinkTravelTime(link1, 7*3600.0), EPSILON);
		assertEquals(10.0, ttcalc.getLinkTravelTime(link1, 8*3600.0), EPSILON);
	}

}
