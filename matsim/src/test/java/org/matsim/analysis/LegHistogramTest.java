/* *********************************************************************** *
 * project: org.matsim.*
 * LegHistogramTest.java
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

package org.matsim.analysis;

import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class LegHistogramTest extends MatsimTestCase {

	/**
	 * Tests that different modes of transport are recognized and accounted
	 * accordingly.  Also tests that modes not defined as constants are
	 * handled correctly.
	 */
	public void testDeparturesMiscModes() {
		NetworkLayer network = new NetworkLayer();
		Node node1 = network.createAndAddNode(new IdImpl(1), new CoordImpl(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl(2), new CoordImpl(1000, 0));
		Link link = network.createAndAddLink(new IdImpl(1), node1, node2, 1000.0, 100.0, 1.0, 1);

		PersonImpl person1 = new PersonImpl(new IdImpl(1));
		PersonImpl person2 = new PersonImpl(new IdImpl(2));
		LegImpl leg = new org.matsim.core.population.LegImpl(TransportMode.car);
		leg.setDepartureTime(7*3600);
		leg.setTravelTime(Time.UNDEFINED_TIME);
		leg.setArrivalTime(Time.UNDEFINED_TIME);
		LegHistogram histo = new LegHistogram(5*60);
		histo.handleEvent(new AgentDepartureEventImpl(7*3600, person1, link, leg));
		histo.handleEvent(new AgentDepartureEventImpl(7*3600 + 6*60, person2, link, leg));
		leg.setMode(TransportMode.bike);
		histo.handleEvent(new AgentDepartureEventImpl(7*3600 + 6*60, person1, link, leg));
		histo.handleEvent(new AgentDepartureEventImpl(7*3600 + 10*60, person2, link, leg));
		leg.setMode(TransportMode.undefined);
		histo.handleEvent(new AgentDepartureEventImpl(7*3600 + 10*60, person1, link, leg));
		leg.setMode(TransportMode.undefined);
		histo.handleEvent(new AgentDepartureEventImpl(7*3600 + 16*60, person1, link, leg));

		int[] carDepartures = histo.getDepartures(TransportMode.car);
		int[] bikeDepartures = histo.getDepartures(TransportMode.bike);
		int[] undefDepartures = histo.getDepartures(TransportMode.undefined);
		int[] otherDepartures = histo.getDepartures(TransportMode.undefined);
		int[] allDepartures = histo.getDepartures();

		assertEquals(1, carDepartures[7*12]);
		assertEquals(1, allDepartures[7*12]);

		assertEquals(1, carDepartures[7*12+1]);
		assertEquals(1, bikeDepartures[7*12+1]);
		assertEquals(2, allDepartures[7*12+1]);

		assertEquals(1, bikeDepartures[7*12+2]);
		assertEquals(1, undefDepartures[7*12+2]);
		assertEquals(2, allDepartures[7*12+2]);

		assertEquals(1, otherDepartures[7*12+3]);
		assertEquals(1, allDepartures[7*12+3]);
	}

	/**
	 * Tests that the constructor-parameter <code>nofBins</code> is correctly
	 * taken into account and that times larger than what is covered by the bins
	 * do not lead to an exception.
	 */
	public void testNofBins() {
		NetworkLayer network = new NetworkLayer();
		Node node1 = network.createAndAddNode(new IdImpl(1), new CoordImpl(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl(2), new CoordImpl(1000, 0));
		Link link = network.createAndAddLink(new IdImpl(1), node1, node2, 1000.0, 100.0, 1.0, 1);

		PersonImpl person1 = new PersonImpl(new IdImpl(1));
		LegImpl leg = new org.matsim.core.population.LegImpl(TransportMode.car);
		leg.setDepartureTime(7*3600);
		leg.setTravelTime(Time.UNDEFINED_TIME);
		leg.setArrivalTime(Time.UNDEFINED_TIME);
		
		LegHistogram histo = new LegHistogram(5*60, 10); // latest time-bin: 2700-2999

		assertEquals(11, histo.getDepartures().length);

		histo.handleEvent(new AgentDepartureEventImpl(2700, person1, link, leg));
		histo.handleEvent(new AgentArrivalEventImpl(2999, person1, link, leg));
		leg.setMode(TransportMode.train);
		histo.handleEvent(new AgentDepartureEventImpl(3000, person1, link, leg));
		histo.handleEvent(new AgentArrivalEventImpl(3001, person1, link, leg));
		leg.setMode(TransportMode.bus);
		histo.handleEvent(new AgentDepartureEventImpl(3600, person1, link, leg));
		histo.handleEvent(new AgentArrivalEventImpl(7200, person1, link, leg));

		assertEquals(1, histo.getDepartures(TransportMode.car)[9]);
		assertEquals(1, histo.getArrivals(TransportMode.car)[9]);

		assertEquals(1, histo.getDepartures(TransportMode.train)[10]);
		assertEquals(1, histo.getArrivals(TransportMode.train)[10]);
		assertEquals(1, histo.getDepartures(TransportMode.bus)[10]);
		assertEquals(1, histo.getArrivals(TransportMode.bus)[10]);

		assertEquals(2, histo.getDepartures()[10]);
		assertEquals(2, histo.getArrivals()[10]);
	}

	public void testReset() {
		NetworkLayer network = new NetworkLayer();
		Node node1 = network.createAndAddNode(new IdImpl(1), new CoordImpl(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl(2), new CoordImpl(1000, 0));
		Link link = network.createAndAddLink(new IdImpl(1), node1, node2, 1000.0, 100.0, 1.0, 1);

		PersonImpl person1 = new PersonImpl(new IdImpl(1));
		LegImpl leg = new org.matsim.core.population.LegImpl(TransportMode.car);
		leg.setDepartureTime(7*3600);
		leg.setTravelTime(Time.UNDEFINED_TIME);
		leg.setArrivalTime(Time.UNDEFINED_TIME);
		
		LegHistogram histo = new LegHistogram(5*60);

		histo.handleEvent(new AgentDepartureEventImpl(7*3600, person1, link, leg));
		histo.handleEvent(new AgentArrivalEventImpl(7*3600 + 6*60, person1, link, leg));
		leg.setMode(TransportMode.train);
		histo.handleEvent(new AgentDepartureEventImpl(8*3600, person1, link, leg));
		histo.handleEvent(new AgentArrivalEventImpl(8*3600 + 11*60, person1, link, leg));

		Set<TransportMode> modes = histo.getLegModes();
		assertEquals(2, modes.size());
		assertTrue(modes.contains(TransportMode.car));
		assertTrue(modes.contains(TransportMode.train));
		assertFalse(modes.contains(TransportMode.bike));

		histo.reset(1);
		modes = histo.getLegModes();
		assertEquals("After reset, there should be 0 known leg-modes", 0, modes.size());
		assertFalse(modes.contains(TransportMode.car));
	}
}
