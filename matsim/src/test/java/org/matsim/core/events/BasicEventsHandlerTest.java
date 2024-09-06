/* *********************************************************************** *
 * project: org.matsim.*
 * EventsHandlerTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.events;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

public class BasicEventsHandlerTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testLinkEnterEventHandler() {
		EventsManager events = EventsUtils.createEventsManager();
		MyLinkEnterEventHandler handler = new MyLinkEnterEventHandler();
		events.addHandler(handler);
		events.initProcessing();

		Network network = NetworkUtils.createNetwork();
        Node node1 = network.getFactory().createNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = network.getFactory().createNode(Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		final Node from = node1;
		final Node to = node2;
		final Network network1 = network;
		NetworkFactory r = network.getFactory();
		Link link1 = NetworkUtils.createLink(Id.create(1, Link.class), from, to, network1, 1000.0, 10.0, 3600.0, (double) 0);

		events.processEvent(new LinkEnterEvent(8.0*3600, Id.create("veh", Vehicle.class), link1.getId()));
		events.finishProcessing();
		assertEquals(1, handler.counter, "expected number of handled events wrong.");
	}


	/*package*/ static class MyLinkEnterEventHandler implements LinkEnterEventHandler {

		/*package*/ int counter = 0;

		@Override
		public void handleEvent(LinkEnterEvent event) {
			this.counter++;
		}

		@Override
		public void reset(int iteration) {
			this.counter = 0;
		}

	}

}
