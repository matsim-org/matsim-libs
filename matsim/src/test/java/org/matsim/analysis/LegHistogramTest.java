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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class LegHistogramTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	/**
	 * Tests that different modes of transport are recognized and accounted
	 * accordingly.  Also tests that modes not defined as constants are
	 * handled correctly.
	 */
	@Test
	void testDeparturesMiscModes() {
		Network network = NetworkUtils.createNetwork();
        Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;
		Link link = NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), fromNode, toNode, 1000.0, 100.0, 1.0, (double) 1 );
		Id<Link> linkId = link.getId();

		Person person1 = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Id<Person> person1Id = person1.getId();
		Person person2 = PopulationUtils.getFactory().createPerson(Id.create(2, Person.class));
		Id<Person> person2Id = person2.getId();
		Leg leg = PopulationUtils.createLeg(TransportMode.car);
		leg.setDepartureTime(7*3600);
		leg.setTravelTimeUndefined();
		LegHistogram histo = new LegHistogram(5*60);
		histo.handleEvent(new PersonDepartureEvent(7*3600, person1Id, linkId, leg.getMode(), leg.getMode()));
		histo.handleEvent(new PersonDepartureEvent(7*3600 + 6*60, person2Id, linkId, leg.getMode(), leg.getMode()));
		leg.setMode(TransportMode.bike);
		histo.handleEvent(new PersonDepartureEvent(7*3600 + 6*60, person1Id, linkId, leg.getMode(), leg.getMode()));
		histo.handleEvent(new PersonDepartureEvent(7*3600 + 10*60, person2Id, linkId, leg.getMode(), leg.getMode()));
		leg.setMode("undefined");
		histo.handleEvent(new PersonDepartureEvent(7*3600 + 10*60, person1Id, linkId, leg.getMode(), leg.getMode()));
		leg.setMode("undefined");
		histo.handleEvent(new PersonDepartureEvent(7*3600 + 16*60, person1Id, linkId, leg.getMode(), leg.getMode()));

		int[] carDepartures = histo.getDepartures(TransportMode.car);
		int[] bikeDepartures = histo.getDepartures(TransportMode.bike);
		int[] undefDepartures = histo.getDepartures("undefined");
		int[] otherDepartures = histo.getDepartures("undefined");
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
	@Test
	void testNofBins() {
        Network network = NetworkUtils.createNetwork();
        Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;
		Link link = NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), fromNode, toNode, 1000.0, 100.0, 1.0, (double) 1 );
		Id<Link> linkId = link.getId();

		Person person1 = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Id<Person> person1Id = person1.getId();
		Leg leg = PopulationUtils.createLeg(TransportMode.car);
		leg.setDepartureTime(7*3600);
		leg.setTravelTimeUndefined();

		LegHistogram histo = new LegHistogram(5*60, 10); // latest time-bin: 2700-2999

		assertEquals(11, histo.getDepartures().length);

		histo.handleEvent(new PersonDepartureEvent(2700, person1Id, linkId, leg.getMode(), leg.getMode()));
		histo.handleEvent(new PersonArrivalEvent(2999, person1Id, linkId, leg.getMode()));
		leg.setMode("train");
		histo.handleEvent(new PersonDepartureEvent(3000, person1Id, linkId, leg.getMode(), leg.getMode()));
		histo.handleEvent(new PersonArrivalEvent(3001, person1Id, linkId, leg.getMode()));
		leg.setMode("bus");
		histo.handleEvent(new PersonDepartureEvent(3600, person1Id, linkId, leg.getMode(), leg.getMode()));
		histo.handleEvent(new PersonArrivalEvent(7200, person1Id, linkId, leg.getMode()));

		assertEquals(1, histo.getDepartures(TransportMode.car)[9]);
		assertEquals(1, histo.getArrivals(TransportMode.car)[9]);

		assertEquals(1, histo.getDepartures("train")[10]);
		assertEquals(1, histo.getArrivals("train")[10]);
		assertEquals(1, histo.getDepartures("bus")[10]);
		assertEquals(1, histo.getArrivals("bus")[10]);

		assertEquals(2, histo.getDepartures()[10]);
		assertEquals(2, histo.getArrivals()[10]);
	}

	@Test
	void testReset() {
        Network network = NetworkUtils.createNetwork();
        Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;
		Link link = NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), fromNode, toNode, 1000.0, 100.0, 1.0, (double) 1 );
		Id<Link> linkId = link.getId();

		Person person1 = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Id<Person> person1Id = person1.getId();
		Leg leg = PopulationUtils.createLeg(TransportMode.car);
		leg.setDepartureTime(7*3600);
		leg.setTravelTimeUndefined();

		LegHistogram histo = new LegHistogram(5*60);

		histo.handleEvent(new PersonDepartureEvent(7*3600, person1Id, linkId, leg.getMode(), leg.getMode()));
		histo.handleEvent(new PersonArrivalEvent(7*3600 + 6*60, person1Id, linkId, leg.getMode()));
		leg.setMode("train");
		histo.handleEvent(new PersonDepartureEvent(8*3600, person1Id, linkId, leg.getMode(), leg.getMode()));
		histo.handleEvent(new PersonArrivalEvent(8*3600 + 11*60, person1Id, linkId, leg.getMode()));

		Set<String> modes = histo.getLegModes();
		assertEquals(2, modes.size());
		assertTrue(modes.contains(TransportMode.car));
		assertTrue(modes.contains("train"));
		assertFalse(modes.contains(TransportMode.bike));

		histo.reset(1);
		modes = histo.getLegModes();
		assertEquals(0, modes.size(), "After reset, there should be 0 known leg-modes");
		assertFalse(modes.contains(TransportMode.car));
	}
}
