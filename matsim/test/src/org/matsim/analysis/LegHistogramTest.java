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

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.misc.Time;

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
		Node node1 = network.createNode(new IdImpl(1), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl(2), new CoordImpl(1000, 0));
		Link link = network.createLink(new IdImpl(1), node1, node2, 1000.0, 100.0, 1.0, 1);

		Person person1 = new PersonImpl(new IdImpl(1));
		Person person2 = new PersonImpl(new IdImpl(2));
		Leg leg = new Leg(BasicLeg.Mode.car);
		leg.setDepartureTime(7*3600);
		leg.setTravelTime(Time.UNDEFINED_TIME);
		leg.setArrivalTime(Time.UNDEFINED_TIME);
		LegHistogram histo = new LegHistogram(5*60);
		histo.handleEvent(new AgentDepartureEvent(7*3600, person1, link, leg));
		histo.handleEvent(new AgentDepartureEvent(7*3600 + 6*60, person2, link, leg));
		leg.setMode(BasicLeg.Mode.bike);
		histo.handleEvent(new AgentDepartureEvent(7*3600 + 6*60, person1, link, leg));
		histo.handleEvent(new AgentDepartureEvent(7*3600 + 10*60, person2, link, leg));
		leg.setMode(BasicLeg.Mode.undefined);
		histo.handleEvent(new AgentDepartureEvent(7*3600 + 10*60, person1, link, leg));
		leg.setMode(BasicLeg.Mode.undefined);
		histo.handleEvent(new AgentDepartureEvent(7*3600 + 16*60, person1, link, leg));

		int[] carDepartures = histo.getDepartures(BasicLeg.Mode.car);
		int[] bikeDepartures = histo.getDepartures(BasicLeg.Mode.bike);
		int[] undefDepartures = histo.getDepartures(BasicLeg.Mode.undefined);
		int[] otherDepartures = histo.getDepartures(BasicLeg.Mode.undefined);
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
		Node node1 = network.createNode(new IdImpl(1), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl(2), new CoordImpl(1000, 0));
		Link link = network.createLink(new IdImpl(1), node1, node2, 1000.0, 100.0, 1.0, 1);

		Person person1 = new PersonImpl(new IdImpl(1));
		Leg leg = new Leg(BasicLeg.Mode.car);
		leg.setDepartureTime(7*3600);
		leg.setTravelTime(Time.UNDEFINED_TIME);
		leg.setArrivalTime(Time.UNDEFINED_TIME);
		
		LegHistogram histo = new LegHistogram(5*60, 10); // latest time-bin: 2700-2999

		assertEquals(11, histo.getDepartures().length);

		histo.handleEvent(new AgentDepartureEvent(2700, person1, link, leg));
		histo.handleEvent(new AgentArrivalEvent(2999, person1, link, leg));
		leg.setMode(BasicLeg.Mode.train);
		histo.handleEvent(new AgentDepartureEvent(3000, person1, link, leg));
		histo.handleEvent(new AgentArrivalEvent(3001, person1, link, leg));
		leg.setMode(BasicLeg.Mode.bus);
		histo.handleEvent(new AgentDepartureEvent(3600, person1, link, leg));
		histo.handleEvent(new AgentArrivalEvent(7200, person1, link, leg));

		assertEquals(1, histo.getDepartures(BasicLeg.Mode.car)[9]);
		assertEquals(1, histo.getArrivals(BasicLeg.Mode.car)[9]);

		assertEquals(1, histo.getDepartures(BasicLeg.Mode.train)[10]);
		assertEquals(1, histo.getArrivals(BasicLeg.Mode.train)[10]);
		assertEquals(1, histo.getDepartures(BasicLeg.Mode.bus)[10]);
		assertEquals(1, histo.getArrivals(BasicLeg.Mode.bus)[10]);

		assertEquals(2, histo.getDepartures()[10]);
		assertEquals(2, histo.getArrivals()[10]);
	}

	public void testReset() {
		NetworkLayer network = new NetworkLayer();
		Node node1 = network.createNode(new IdImpl(1), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl(2), new CoordImpl(1000, 0));
		Link link = network.createLink(new IdImpl(1), node1, node2, 1000.0, 100.0, 1.0, 1);

		Person person1 = new PersonImpl(new IdImpl(1));
		Leg leg = new Leg(BasicLeg.Mode.car);
		leg.setDepartureTime(7*3600);
		leg.setTravelTime(Time.UNDEFINED_TIME);
		leg.setArrivalTime(Time.UNDEFINED_TIME);
		
		LegHistogram histo = new LegHistogram(5*60);

		histo.handleEvent(new AgentDepartureEvent(7*3600, person1, link, leg));
		histo.handleEvent(new AgentArrivalEvent(7*3600 + 6*60, person1, link, leg));
		leg.setMode(BasicLeg.Mode.train);
		histo.handleEvent(new AgentDepartureEvent(8*3600, person1, link, leg));
		histo.handleEvent(new AgentArrivalEvent(8*3600 + 11*60, person1, link, leg));

		Set<BasicLeg.Mode> modes = histo.getLegModes();
		assertEquals(2, modes.size());
		assertTrue(modes.contains(BasicLeg.Mode.car));
		assertTrue(modes.contains(BasicLeg.Mode.train));
		assertFalse(modes.contains(BasicLeg.Mode.bike));

		histo.reset(1);
		modes = histo.getLegModes();
		assertEquals("After reset, there should be 0 known leg-modes", 0, modes.size());
		assertFalse(modes.contains(BasicLeg.Mode.car));
	}
}
