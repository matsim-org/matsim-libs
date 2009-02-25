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

import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.routes.NodeCarRoute;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.NetworkUtils;

/**
 * @author dgrether
 */
public class AbstractControlInputImplTest extends MatsimTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Gbl.createConfig(null);
	}

	public void testAbstractControlInputImpl() {
		NetworkLayer network = new NetworkLayer();
		MatsimNetworkReader parser = new MatsimNetworkReader(network);
		parser.readFile(getInputDirectory() + "network.xml");

		CarRoute route1 = new NodeCarRoute();
		route1.setNodes(NetworkUtils.getNodes(network, "3 6 7 12"));
		CarRoute route2 = new NodeCarRoute();
		route2.setNodes(NetworkUtils.getNodes(network, "3 8 9 12"));

		//control input test class
		ControlInputTestImpl ci = new ControlInputTestImpl();
		ci.setMainRoute(route1);
		ci.setAlternativeRoute(route2);
		ci.init();

		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		LinkEnterEvent enterEvent = new LinkEnterEvent(0.0, "0", "5", 0);
		ci.handleEvent(enterEvent);
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		enterEvent = new LinkEnterEvent(0.0, "1", "5", 0);
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		LinkLeaveEvent leaveEvent = new LinkLeaveEvent(1.0, "0", "5", 0);
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEvent(1.0, "0", "6", 0);
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		enterEvent = new LinkEnterEvent(1.0, "2", "7", 0);
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new LinkLeaveEvent(2.0, "2", "7", 0);
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEvent(2.0, "2", "8", 0);
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new LinkLeaveEvent(3.0, "2", "8", 0);
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEvent(3.0, "2", "16", 0);
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new LinkLeaveEvent(4.0, "2", "16", 0);
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEvent(4.0, "2", "20", 0);
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new LinkLeaveEvent(4.0, "1", "5", 0);
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEvent(4.0, "1", "6", 0);
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new LinkLeaveEvent(4.0, "1", "5", 0);
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEvent(4.0, "1", "6", 0);
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new LinkLeaveEvent(4.0, "1", "6", 0);
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEvent(4.0, "1", "20", 0);
		ci.handleEvent(enterEvent);
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
    leaveEvent = new LinkLeaveEvent(1.0, "0", "6", 0);
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEvent(1.0, "0", "20", 0);
		ci.handleEvent(enterEvent);
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
	}

}
