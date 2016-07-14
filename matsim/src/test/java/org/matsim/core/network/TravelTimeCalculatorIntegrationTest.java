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

package org.matsim.core.network;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.network.*;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeDataHashMapFactory;
import org.matsim.testcases.MatsimTestCase;

/**
 * This is not a "full" integration test running from files. It rather tests, if the travel time calculator can be
 * used with a time variant network.  
 *
 */
public class TravelTimeCalculatorIntegrationTest extends MatsimTestCase {

	public void testTravelTimeCalculatorArray() {
        for (LinkFactory lf : TimeVariantLinkImplTest.linkFactories(15 * 60, 30 * 3600)) {
    		Config config = loadConfig(null);
    
    		// create a network
    		final NetworkImpl network = NetworkImpl.createNetwork();
    		NetworkFactoryImpl nf = new NetworkFactoryImpl(network);
    		nf.setLinkFactory(lf);
    		network.setFactory(nf);
    		network.setCapacityPeriod(3600.0);
    
    		// the netework has 4 nodes and 3 links, each link by default 100 long and freespeed = 10 --> freespeed travel time = 10.0
    		Node node1 = network.createAndAddNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
    		Node node2 = network.createAndAddNode(Id.create("2", Node.class), new Coord((double) 100, (double) 0));
    		Node node3 = network.createAndAddNode(Id.create("3", Node.class), new Coord((double) 200, (double) 0));
    		Node node4 = network.createAndAddNode(Id.create("4", Node.class), new Coord((double) 300, (double) 0));
    		Link link1 = network.createAndAddLink(Id.create("1", Link.class), node1, node2, 100, 10, 3600, 1);
    		Link link2 = network.createAndAddLink(Id.create("2", Link.class), node2, node3, 100, 10, 3600, 1);
    		network.createAndAddLink(Id.create("3", Link.class), node3, node4, 100, 10, 3600, 1);
    
    		// add a freespeed change to 20 at 8am.
    		NetworkChangeEvent change = nf.createNetworkChangeEvent(8*3600.0);
    		change.addLink(link2);
    		change.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, 20));
    		network.addNetworkChangeEvent(change);
    
    		// create a travel time calculator object
    		TravelTime ttcalc = new TravelTimeCalculator(network,config.travelTimeCalculator()).getLinkTravelTimes();
    
    		// do the tests
    		assertEquals(10.0, ttcalc.getLinkTravelTime(link2, 7*3600.0, null, null), EPSILON);
    		assertEquals(5.0, ttcalc.getLinkTravelTime(link2, 8*3600.0, null, null), EPSILON);
    		assertEquals(10.0, ttcalc.getLinkTravelTime(link1, 7*3600.0, null, null), EPSILON);
    		assertEquals(10.0, ttcalc.getLinkTravelTime(link1, 8*3600.0, null, null), EPSILON);
        }
	}

	public void testTravelTimeCalculatorHashMap() {
        for (LinkFactory lf : TimeVariantLinkImplTest.linkFactories(15 * 60, 30 * 3600)) {
    		Config config = loadConfig(null);
    
    		// create a network
    		final NetworkImpl network = NetworkImpl.createNetwork();
    		NetworkFactoryImpl nf = new NetworkFactoryImpl(network);
    		nf.setLinkFactory(lf);
    		network.setFactory(nf);
    		network.setCapacityPeriod(3600.0);
    
    		// the netework has 4 nodes and 3 links, each link by default 100 long and freespeed = 10 --> freespeed travel time = 10.0
    		Node node1 = network.createAndAddNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
    		Node node2 = network.createAndAddNode(Id.create("2", Node.class), new Coord((double) 100, (double) 0));
    		Node node3 = network.createAndAddNode(Id.create("3", Node.class), new Coord((double) 200, (double) 0));
    		Node node4 = network.createAndAddNode(Id.create("4", Node.class), new Coord((double) 300, (double) 0));
    		Link link1 = network.createAndAddLink(Id.create("1", Link.class), node1, node2, 100, 10, 3600, 1);
    		Link link2 = network.createAndAddLink(Id.create("2", Link.class), node2, node3, 100, 10, 3600, 1);
    		network.createAndAddLink(Id.create("3", Link.class), node3, node4, 100, 10, 3600, 1);
    
    		// add a freespeed change to 20 at 8am.
    		NetworkChangeEvent change = nf.createNetworkChangeEvent(8*3600.0);
    		change.addLink(link2);
    		change.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, 20));
    		network.addNetworkChangeEvent(change);
    
    		// create a travel time calculator object
    		TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, 15*60, 30*3600, config.travelTimeCalculator());
    		ttcalc.setTravelTimeDataFactory(new TravelTimeDataHashMapFactory(network));
    
    		// do the tests
    		assertEquals(10.0, ttcalc.getLinkTravelTimes().getLinkTravelTime(link2, 7*3600.0, null, null), EPSILON);
    		assertEquals(5.0, ttcalc.getLinkTravelTimes().getLinkTravelTime(link2, 8*3600.0, null, null), EPSILON);
    		assertEquals(10.0, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7*3600.0, null, null), EPSILON);
    		assertEquals(10.0, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 8*3600.0, null, null), EPSILON);
        }
	}
}
