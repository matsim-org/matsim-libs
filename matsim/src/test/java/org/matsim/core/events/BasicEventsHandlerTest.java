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

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

public class BasicEventsHandlerTest extends MatsimTestCase {

	public void testLinkEnterEventHandler() {
		EventsManagerImpl events = new EventsManagerImpl();
		MyLinkEnterEventHandler handler = new MyLinkEnterEventHandler();
		events.addHandler(handler);
		events.printEventHandlers();
		
		NetworkLayer network = new NetworkLayer();
		Node node1 = network.getFactory().createNode(new IdImpl(1), new CoordImpl(0, 0));
		Node node2 = network.getFactory().createNode(new IdImpl(2), new CoordImpl(1000, 0));
		LinkImpl link1 = network.getFactory().createLink(new IdImpl(1), node1, node2, network, 1000.0, 10.0, 3600.0, 0);

		events.processEvent(new LinkEnterEventImpl(8.0*3600, new PersonImpl(new IdImpl(1)).getId(), link1.getId()));
		assertEquals("expected number of handled events wrong.", 1, handler.counter);
	}
	
	
	/*package*/ static class MyLinkEnterEventHandler implements LinkEnterEventHandler {

		/*package*/ int counter = 0;
		
		public void handleEvent(LinkEnterEvent event) {
			this.counter++;
		}

		public void reset(int iteration) {
			this.counter = 0;
		}
		
	}
	
}
