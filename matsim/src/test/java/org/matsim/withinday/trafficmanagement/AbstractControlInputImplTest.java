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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.population.routes.NodeNetworkRouteImpl;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author dgrether
 */
public class AbstractControlInputImplTest extends MatsimTestCase {

	public void testAbstractControlInputImpl() {
		Scenario scenario = new ScenarioImpl(super.loadConfig(null));
		Network network = scenario.getNetwork();
		MatsimNetworkReader parser = new MatsimNetworkReader(scenario);
		parser.readFile(getInputDirectory() + "network.xml");

		NetworkRouteWRefs route1 = new NodeNetworkRouteImpl();
		route1.setNodes(NetworkUtils.getNodes(network, "3 6 7 12"));
		NetworkRouteWRefs route2 = new NodeNetworkRouteImpl();
		route2.setNodes(NetworkUtils.getNodes(network, "3 8 9 12"));

		//control input test class
		ControlInputTestImpl ci = new ControlInputTestImpl(network);
		ci.setMainRoute(route1);
		ci.setAlternativeRoute(route2);
		ci.init();

		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		LinkEnterEventImpl enterEvent = new LinkEnterEventImpl(0.0, new IdImpl("0"), new IdImpl("5"));
		ci.handleEvent(enterEvent);
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		enterEvent = new LinkEnterEventImpl(0.0, new IdImpl("1"), new IdImpl("5"));
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		LinkLeaveEventImpl leaveEvent = new LinkLeaveEventImpl(1.0, new IdImpl("0"), new IdImpl("5"));
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEventImpl(1.0, new IdImpl("0"), new IdImpl("6"));
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		enterEvent = new LinkEnterEventImpl(1.0, new IdImpl("2"), new IdImpl("7"));
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new LinkLeaveEventImpl(2.0, new IdImpl("2"), new IdImpl("7"));
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEventImpl(2.0, new IdImpl("2"), new IdImpl("8"));
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new LinkLeaveEventImpl(3.0, new IdImpl("2"), new IdImpl("8"));
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEventImpl(3.0, new IdImpl("2"), new IdImpl("16"));
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new LinkLeaveEventImpl(4.0, new IdImpl("2"), new IdImpl("16"));
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEventImpl(4.0, new IdImpl("2"), new IdImpl("20"));
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new LinkLeaveEventImpl(4.0, new IdImpl("1"), new IdImpl("5"));
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEventImpl(4.0, new IdImpl("1"), new IdImpl("6"));
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new LinkLeaveEventImpl(4.0, new IdImpl("1"), new IdImpl("5"));
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEventImpl(4.0, new IdImpl("1"), new IdImpl("6"));
		ci.handleEvent(enterEvent);
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
		leaveEvent = new LinkLeaveEventImpl(4.0, new IdImpl("1"), new IdImpl("6"));
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEventImpl(4.0, new IdImpl("1"), new IdImpl("20"));
		ci.handleEvent(enterEvent);
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
    leaveEvent = new LinkLeaveEventImpl(1.0, new IdImpl("0"), new IdImpl("6"));
		ci.handleEvent(leaveEvent);
		enterEvent = new LinkEnterEventImpl(1.0, new IdImpl("0"), new IdImpl("20"));
		ci.handleEvent(enterEvent);
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
	}

}
