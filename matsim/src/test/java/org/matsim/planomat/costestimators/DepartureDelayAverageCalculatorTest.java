/* *********************************************************************** *
 * project: org.matsim.*
 * DepartureDelayAverageCalculatorTest.java
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

package org.matsim.planomat.costestimators;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.EventImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

public class DepartureDelayAverageCalculatorTest extends MatsimTestCase {

	private NetworkLayer network = null;
	private static final Id LINK_ID = new IdImpl("1");
	private static final Id PERSON_ID = new IdImpl("1");
	private static final int TIME_BIN_SIZE = 900;

	@Override
	protected void setUp() throws Exception {

		super.setUp();

		// we need a network with just one link
		network = new NetworkLayer();

		double fromX = 100.0;
		double fromY = 100.0;
		double toX = 100.0;
		double toY = 200.0;
		Node fromNode = network.createAndAddNode(new IdImpl("1"), new CoordImpl(fromX, fromY));
		Node toNode = network.createAndAddNode(new IdImpl("2"), new CoordImpl(toX, toY));
		network.createAndAddLink(LINK_ID, fromNode, toNode, 999.9, 50.0 / 3.6, 1000, 1);
	}

	@Override
	protected void tearDown() throws Exception {
		this.network = null;
		super.tearDown();
	}

	public void testGetLinkDepartureDelay() {

		double depDelay = 0.0;

		EventsManagerImpl events = new EventsManagerImpl();
		DepartureDelayAverageCalculator testee = new DepartureDelayAverageCalculator(network, TIME_BIN_SIZE);
		events.addHandler(testee);
		events.printEventHandlers();

		// this gives a delay of 36s
		AgentDepartureEventImpl depEvent = new AgentDepartureEventImpl(6.01 * 3600, PERSON_ID, LINK_ID, TransportMode.car);
		LinkLeaveEventImpl leaveEvent = new LinkLeaveEventImpl(6.02 * 3600, PERSON_ID, LINK_ID);

		for (EventImpl event : new EventImpl[]{depEvent, leaveEvent}) {
			events.processEvent(event);
		}

		depDelay = testee.getLinkDepartureDelay(network.getLinks().get(new IdImpl("1")), 6.00 * 3600);
		assertEquals(depDelay, 36.0, EPSILON);

		// let's add another delay of 72s, should result in an average of 54s
		depEvent = new AgentDepartureEventImpl(6.02 * 3600, PERSON_ID, LINK_ID, TransportMode.car);
		leaveEvent = new LinkLeaveEventImpl(6.04 * 3600, PERSON_ID, LINK_ID);

		for (EventImpl event : new EventImpl[]{depEvent, leaveEvent}) {
			events.processEvent(event);
		}

		depDelay = testee.getLinkDepartureDelay(network.getLinks().get(new IdImpl("1")), 6.00 * 3600);
		assertEquals(depDelay, 54.0, EPSILON);

		// the time interval for the previously tested events was for departure times from 6.00 to 6.25
		// for other time intervals, we don't have event information, so estimated delay should be 0s
		depDelay = testee.getLinkDepartureDelay(network.getLinks().get(new IdImpl("1")), 5.9 * 3600);
		assertEquals(depDelay, 0.0, EPSILON);
		depDelay = testee.getLinkDepartureDelay(network.getLinks().get(new IdImpl("1")), 6.26 * 3600);
		assertEquals(depDelay, 0.0, EPSILON);

	}

}
