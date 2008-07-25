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

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.BasicEvent;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.shared.Coord;

public class DepartureDelayAverageCalculatorTest extends MatsimTestCase {

	private NetworkLayer network = null;
	private final String LINK_ID = "1";
	private final String PERSON_ID = "1";
	private static final int TIME_BIN_SIZE = 900;

	@Override
	protected void setUp() throws Exception {

		super.setUp();
		
		// we need a network with just one link
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);

		double fromX = 100.0;
		double fromY = 100.0;
		double toX = 100.0;
		double toY = 200.0;
		Node fromNode = network.createNode(new IdImpl("1"), new Coord(fromX, fromY));
		Node toNode = network.createNode(new IdImpl("2"), new Coord(toX, toY));
		network.createLink(new IdImpl(LINK_ID), fromNode, toNode, 999.9, 50.0 / 3.6, 1000, 1);
		
	}

	public void testGetLinkDepartureDelay() {
		
		double depDelay = 0.0;

		Events events = new Events();
		DepartureDelayAverageCalculator testee = new DepartureDelayAverageCalculator(network, TIME_BIN_SIZE);
		events.addHandler(testee);
		events.printEventHandlers();
		
		// this gives a delay of 36s
		EventAgentDeparture depEvent = new EventAgentDeparture(6.01 * 3600, PERSON_ID, 0, LINK_ID);
		EventLinkLeave leaveEvent = new EventLinkLeave(6.02 * 3600, PERSON_ID, 0, LINK_ID);
		
		for (BasicEvent event : new BasicEvent[]{depEvent, leaveEvent}) {
			events.processEvent(event);
		}
		
		depDelay = testee.getLinkDepartureDelay(network.getLink(new IdImpl("1")), 6.00 * 3600);
		assertEquals(depDelay, 36.0);
		
		// let's add another delay of 72s, should result in an average of 54s
		depEvent = new EventAgentDeparture(6.02 * 3600, PERSON_ID, 0, LINK_ID);
		leaveEvent = new EventLinkLeave(6.04 * 3600, PERSON_ID, 0, LINK_ID);
		
		for (BasicEvent event : new BasicEvent[]{depEvent, leaveEvent}) {
			events.processEvent(event);
		}

		depDelay = testee.getLinkDepartureDelay(network.getLink(new IdImpl("1")), 6.00 * 3600);
		assertEquals(depDelay, 54.0);

		// the time interval for the previously tested events was for departure times from 6.00 to 6.25
		// for other time intervals, we don't have event information, so estimated delay should be 0s
		depDelay = testee.getLinkDepartureDelay(network.getLink(new IdImpl("1")), 5.9 * 3600);
		assertEquals(depDelay, 0.0);
		depDelay = testee.getLinkDepartureDelay(network.getLink(new IdImpl("1")), 6.26 * 3600);
		assertEquals(depDelay, 0.0);
		
	}
	
}
