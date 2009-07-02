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

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.NodeNetworkRoute;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.testcases.MatsimTestCase;

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

		NetworkRoute route1 = new NodeNetworkRoute();
		route1.setNodes(NetworkUtils.getNodes(network, "3 6 7 12"));
		NetworkRoute route2 = new NodeNetworkRoute();
		route2.setNodes(NetworkUtils.getNodes(network, "3 8 9 12"));

		//control input test class
		ControlInputTestImpl ci = new ControlInputTestImpl();
		ci.setMainRoute(route1);
		ci.setAlternativeRoute(route2);
		ci.init();

		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		LinkEnterEvent enterEvent = new LinkEnterEvent(0.0, new IdImpl("0"), new IdImpl("5"));
		ci.handleEvent(enterEvent);
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		enterEvent = new LinkEnterEvent(0.0, new IdImpl("1"), new IdImpl("5"));
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		LinkLeaveEvent leaveEvent = new LinkLeaveEvent(1.0, new IdImpl("0"), new IdImpl("5"));
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEvent(1.0, new IdImpl("0"), new IdImpl("6"));
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		enterEvent = new LinkEnterEvent(1.0, new IdImpl("2"), new IdImpl("7"));
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new LinkLeaveEvent(2.0, new IdImpl("2"), new IdImpl("7"));
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEvent(2.0, new IdImpl("2"), new IdImpl("8"));
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new LinkLeaveEvent(3.0, new IdImpl("2"), new IdImpl("8"));
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEvent(3.0, new IdImpl("2"), new IdImpl("16"));
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new LinkLeaveEvent(4.0, new IdImpl("2"), new IdImpl("16"));
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEvent(4.0, new IdImpl("2"), new IdImpl("20"));
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new LinkLeaveEvent(4.0, new IdImpl("1"), new IdImpl("5"));
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEvent(4.0, new IdImpl("1"), new IdImpl("6"));
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new LinkLeaveEvent(4.0, new IdImpl("1"), new IdImpl("5"));
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEvent(4.0, new IdImpl("1"), new IdImpl("6"));
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new LinkLeaveEvent(4.0, new IdImpl("1"), new IdImpl("6"));
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEvent(4.0, new IdImpl("1"), new IdImpl("20"));
		ci.handleEvent(enterEvent);
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
    leaveEvent = new LinkLeaveEvent(1.0, new IdImpl("0"), new IdImpl("6"));
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEvent(1.0, new IdImpl("0"), new IdImpl("20"));
		ci.handleEvent(enterEvent);
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
	}

}
