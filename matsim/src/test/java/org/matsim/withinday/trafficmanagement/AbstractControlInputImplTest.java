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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
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

		Id agentId0 = scenario.createId("0");
		Id agentId1 = scenario.createId("1");
		Id agentId2 = scenario.createId("2");

		Id linkId5 = scenario.createId("5");
		Id linkId6 = scenario.createId("6");
		Id linkId7 = scenario.createId("7");
		Id linkId8 = scenario.createId("8");
		Id linkId14 = scenario.createId("14");
		Id linkId16 = scenario.createId("16");
		Id linkId20 = scenario.createId("20");

		Link link5 = network.getLinks().get(linkId5);
		Link link7 = network.getLinks().get(linkId7);
		Link link14 = network.getLinks().get(linkId14);
		Link link16 = network.getLinks().get(linkId16);
		NetworkRoute route1 = new LinkNetworkRouteImpl(link5.getId(), link14.getId());
		route1.setLinkIds(link5.getId(), NetworkUtils.getLinkIds("6"), link14.getId());
		NetworkRoute route2 = new LinkNetworkRouteImpl(link7.getId(), link16.getId());
		route2.setLinkIds(link7.getId(), NetworkUtils.getLinkIds("8"), link16.getId());

		//control input test class
		ControlInputTestImpl ci = new ControlInputTestImpl(network, getOutputDirectory());
		ci.setMainRoute(route1);
		ci.setAlternativeRoute(route2);
		ci.init();

		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));

		ci.handleEvent(new LinkEnterEventImpl(0.0, agentId0, linkId5));

		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));

		ci.handleEvent(new LinkEnterEventImpl(0.0, agentId1, linkId5));

		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));

		ci.handleEvent(new LinkLeaveEventImpl(1.0, agentId0, linkId5));
		ci.handleEvent(new LinkEnterEventImpl(1.0, agentId0, linkId6));

		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));

		ci.handleEvent(new LinkEnterEventImpl(1.0, agentId2, linkId7));

		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route2));

		ci.handleEvent(new LinkLeaveEventImpl(2.0, agentId2, linkId7));
		ci.handleEvent(new LinkEnterEventImpl(2.0, agentId2, linkId8));

		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route2));

		ci.handleEvent(new LinkLeaveEventImpl(3.0, agentId2, linkId8));
		ci.handleEvent(new LinkEnterEventImpl(3.0, agentId2, linkId16));

		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route2));

		ci.handleEvent(new LinkLeaveEventImpl(4.0, agentId2, linkId16));
		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		ci.handleEvent(new LinkEnterEventImpl(4.0, agentId2, linkId20));

		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));

		ci.handleEvent(new LinkLeaveEventImpl(4.0, agentId1, linkId5));
		ci.handleEvent(new LinkEnterEventImpl(4.0, agentId1, linkId6));

		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));

		ci.handleEvent(new LinkLeaveEventImpl(4.0, agentId1, linkId5));
		ci.handleEvent(new LinkEnterEventImpl(4.0, agentId1, linkId6));

		assertEquals(2, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));

		ci.handleEvent(new LinkLeaveEventImpl(4.0, agentId1, linkId6));
		ci.handleEvent(new LinkEnterEventImpl(4.0, agentId1, linkId20));

		assertEquals(1, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));

		ci.handleEvent(new LinkLeaveEventImpl(1.0, agentId0, linkId6));
		ci.handleEvent(new LinkEnterEventImpl(1.0, agentId0, linkId20));

		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route1));
		assertEquals(0, ci.getNumberOfVehiclesOnRoute(route2));
	}

}
