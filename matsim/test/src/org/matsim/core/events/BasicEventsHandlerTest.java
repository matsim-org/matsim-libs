/* *********************************************************************** *
 * project: org.matsim.*
 * BasicEventsHandlerTest.java
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

import org.matsim.api.basic.v01.events.BasicLinkEnterEvent;
import org.matsim.api.basic.v01.events.handler.BasicLinkEnterEventHandler;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

public class BasicEventsHandlerTest extends MatsimTestCase {

	public void testBasicLinkEnterEventHandler() {
		Events events = new Events();
		MyBasicLinkEnterEventHandler handler = new MyBasicLinkEnterEventHandler();
		events.addHandler(handler);
		events.printEventHandlers();
		
		NetworkLayer network = new NetworkLayer();
		Node node1 = network.getFactory().createNode(new IdImpl(1), new CoordImpl(0, 0), null);
		Node node2 = network.getFactory().createNode(new IdImpl(2), new CoordImpl(1000, 0), null);
		Link link1 = network.getFactory().createLink(new IdImpl(1), node1, node2, network, 1000.0, 10.0, 3600.0, 0);

		events.processEvent(new LinkEnterEvent(8.0*3600, new PersonImpl(new IdImpl(1)), link1));
		assertEquals("expected number of handled events wrong.", 1, handler.counter);
	}
	
	
	/*package*/ static class MyBasicLinkEnterEventHandler implements BasicLinkEnterEventHandler {

		/*package*/ int counter = 0;
		
		public void handleEvent(BasicLinkEnterEvent event) {
			this.counter++;
		}

		public void reset(int iteration) {
			this.counter = 0;
		}
		
	}
	
}
