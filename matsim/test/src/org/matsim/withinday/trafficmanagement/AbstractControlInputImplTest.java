/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractControlInputImplTest.java
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

package org.matsim.withinday.trafficmanagement;

import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Route;
import org.matsim.testcases.MatsimTestCase;


/**
 * @author dgrether
 */
public class AbstractControlInputImplTest extends MatsimTestCase {

	private static final String networkPath = "./test/input/org/matsim/withinday/trafficmanagement/AbstractControlInputImplTest/testAbstractControlInputImpl/network.xml";

	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Gbl.createConfig(null);
	}

	public void testAbstractControlInputImpl() {
		NetworkLayer network = new NetworkLayer();
		MatsimNetworkReader parser = new MatsimNetworkReader(network);
		parser.readFile(networkPath);
		Gbl.createWorld().setNetworkLayer(network);

		Route route1 = new Route();
		route1.setRoute("3 6 7 12");
		Route route2 = new Route();
		route2.setRoute("3 8 9 12");

		//control input test class
		ControlInputTestImpl ci = new ControlInputTestImpl();
		ci.setMainRoute(route1);
		ci.setAlternativeRoute(route2);
		ci.init();

		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		EventLinkEnter enterEvent = new EventLinkEnter(0.0, "0", 0, "5");
		ci.handleEvent(enterEvent);
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		enterEvent = new EventLinkEnter(0.0, "1", 0, "5");
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		EventLinkLeave leaveEvent = new EventLinkLeave(1.0, "0", 0, "5");
		ci.handleEvent(leaveEvent);
		enterEvent = new EventLinkEnter(1.0, "0", 0, "6");
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		enterEvent = new EventLinkEnter(1.0, "2", 0, "7");
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new EventLinkLeave(2.0, "2", 0, "7");
		ci.handleEvent(leaveEvent);
		enterEvent = new EventLinkEnter(2.0, "2", 0, "8");
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new EventLinkLeave(3.0, "2", 0, "8");
		ci.handleEvent(leaveEvent);
		enterEvent = new EventLinkEnter(3.0, "2", 0, "16");
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new EventLinkLeave(4.0, "2", 0, "16");
		ci.handleEvent(leaveEvent);
		enterEvent = new EventLinkEnter(4.0, "2", 0, "20");
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new EventLinkLeave(4.0, "1", 0, "5");
		ci.handleEvent(leaveEvent);
		enterEvent = new EventLinkEnter(4.0, "1", 0, "6");
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new EventLinkLeave(4.0, "1", 0, "5");
		ci.handleEvent(leaveEvent);
		enterEvent = new EventLinkEnter(4.0, "1", 0, "6");
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new EventLinkLeave(4.0, "1", 0, "6");
		ci.handleEvent(leaveEvent);
		enterEvent = new EventLinkEnter(4.0, "1", 0, "20");
		ci.handleEvent(enterEvent);
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
    leaveEvent = new EventLinkLeave(1.0, "0", 0, "6");
		ci.handleEvent(leaveEvent);
		enterEvent = new EventLinkEnter(1.0, "0", 0, "20");
		ci.handleEvent(enterEvent);
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
	}

}
