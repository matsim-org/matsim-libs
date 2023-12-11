/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouteTest.java
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

package org.matsim.pt.transitSchedule;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.fakes.FakeLink;

/**
 * @author mrieser
 */
public class TransitRouteTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	private static final Logger log = LogManager.getLogger(TransitRouteTest.class);

	/**
	 * In case we once should have more than one implementation of
	 * {@link TransitRoute}, simply inherit from this test and overwrite
	 * this method to return your own implementation.
	 *
	 * @param id
	 * @param route
	 * @param stops
	 * @param mode
	 * @return a new instance of a TransitRoute with the given attributes
	 */
	protected static TransitRoute createTransitRoute(final Id<TransitRoute> id, final NetworkRoute route, final List<TransitRouteStop> stops, final String mode) {
		return new TransitRouteImpl(id, route, stops, mode);
	}

	@Test
	void testInitialization() {
		Id<TransitRoute> id = Id.create(9791, TransitRoute.class);
		Link fromLink = new FakeLink(Id.create(10, Link.class), null, null);
		Link toLink = new FakeLink(Id.create(5, Link.class), null, null);
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(fromLink.getId(), toLink.getId());
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitRouteStop stop = new TransitRouteStopImpl.Builder().arrivalOffset(50).departureOffset(60).build();
		stops.add(stop);

		TransitRoute tRoute = createTransitRoute(id, route, stops, "train");
		assertEquals(id.toString(), tRoute.getId().toString(), "wrong id.");
		assertEquals(route, tRoute.getRoute(), "wrong route.");
		assertEquals(stops.size(), tRoute.getStops().size());
		assertEquals(stop, tRoute.getStops().get(0));
		assertEquals("train", tRoute.getTransportMode());
	}

	@Test
	void testDescription() {
		Fixture f = new Fixture();
		assertNull(f.tRoute.getDescription());
		String desc = "some random description string.";
		f.tRoute.setDescription(desc);
		assertEquals(desc, f.tRoute.getDescription());
		desc += " [updated]";
		f.tRoute.setDescription(desc);
		assertEquals(desc, f.tRoute.getDescription());
	}

	@Test
	void testTransportMode() {
		Fixture f = new Fixture();
		// test default of Fixture
		assertEquals("train", f.tRoute.getTransportMode());
		// test some different from default
		f.tRoute.setTransportMode("tram");
		assertEquals("tram", f.tRoute.getTransportMode());
	}

	@Test
	void testAddDepartures() {
		Fixture f = new Fixture();
		Departure dep1 = new DepartureImpl(Id.create(1, Departure.class), 7.0*3600);
		Departure dep2 = new DepartureImpl(Id.create(2, Departure.class), 8.0*3600);
		Departure dep3 = new DepartureImpl(Id.create(3, Departure.class), 9.0*3600);
		assertEquals(0, f.tRoute.getDepartures().size());
		f.tRoute.addDeparture(dep1);
		assertEquals(1, f.tRoute.getDepartures().size());
		assertEquals(dep1, f.tRoute.getDepartures().get(dep1.getId()));
		f.tRoute.addDeparture(dep2);
		assertEquals(2, f.tRoute.getDepartures().size());
		assertEquals(dep1, f.tRoute.getDepartures().get(dep1.getId()));
		assertEquals(dep2, f.tRoute.getDepartures().get(dep2.getId()));
		assertNull(f.tRoute.getDepartures().get(dep3.getId()));
		f.tRoute.addDeparture(dep3);
		assertEquals(3, f.tRoute.getDepartures().size());
		assertEquals(dep3, f.tRoute.getDepartures().get(dep3.getId()));
	}

	@Test
	void testAddDeparturesException() {
		Fixture f = new Fixture();
		Departure dep1a = new DepartureImpl(Id.create(1, Departure.class), 7.0*3600);
		Departure dep1b = new DepartureImpl(Id.create(1, Departure.class), 7.0*3600);
		assertEquals(0, f.tRoute.getDepartures().size());
		f.tRoute.addDeparture(dep1a);
		assertEquals(1, f.tRoute.getDepartures().size());
		try {
			f.tRoute.addDeparture(dep1b);
			fail("missing exception.");
		}
		catch (IllegalArgumentException e) {
			log.info("catched expected exception.", e);
		}
	}

	@Test
	void testRemoveDepartures() {
		Fixture f = new Fixture();
		Departure dep1 = new DepartureImpl(Id.create(1, Departure.class), 7.0*3600);
		Departure dep2 = new DepartureImpl(Id.create(2, Departure.class), 8.0*3600);

		f.tRoute.addDeparture(dep1);
		f.tRoute.addDeparture(dep2);
		assertEquals(2, f.tRoute.getDepartures().size());
		assertNotNull(f.tRoute.getDepartures().get(dep1.getId()));
		assertNotNull(f.tRoute.getDepartures().get(dep2.getId()));

		assertTrue(f.tRoute.removeDeparture(dep1));
		assertEquals(1, f.tRoute.getDepartures().size());
		assertNull(f.tRoute.getDepartures().get(dep1.getId()));
		assertNotNull(f.tRoute.getDepartures().get(dep2.getId()));

		assertTrue(f.tRoute.removeDeparture(dep2));
		assertEquals(0, f.tRoute.getDepartures().size());

		f.tRoute.addDeparture(dep1);
		assertEquals(1, f.tRoute.getDepartures().size());
		assertFalse(f.tRoute.removeDeparture(dep2));
		assertEquals(1, f.tRoute.getDepartures().size());
		assertNotNull(f.tRoute.getDepartures().get(dep1.getId()));
	}

	@Test
	void testGetDeparturesImmutable() {
		Fixture f = new Fixture();
		Departure dep1 = new DepartureImpl(Id.create(1, Departure.class), 7.0*3600);
		assertEquals(0, f.tRoute.getDepartures().size());
		try {
			f.tRoute.getDepartures().put(dep1.getId(), dep1);
			fail("missing exception");
		}
		catch (UnsupportedOperationException e) {
			log.info("catched expected exception.", e);
		}
	}

	@Test
	void testRoute() {
		Fixture f = new Fixture();
		Link link1 = new FakeLink(Id.create(1, Link.class), null, null);
		Link link2 = new FakeLink(Id.create(2, Link.class), null, null);
		Link link3 = new FakeLink(Id.create(3, Link.class), null, null);
		NetworkRoute route1 = RouteUtils.createLinkNetworkRouteImpl(link1.getId(), link2.getId());
		NetworkRoute route2 = RouteUtils.createLinkNetworkRouteImpl(link1.getId(), link3.getId());

		f.tRoute.setRoute(route1);
		assertEquals(route1, f.tRoute.getRoute());
		f.tRoute.setRoute(route2);
		assertEquals(route2, f.tRoute.getRoute());
	}

	@Test
	void testStops() {
		Id<TransitRoute> id = Id.create(9791, TransitRoute.class);
		Link fromLink = new FakeLink(Id.create(10, Link.class), null, null);
		Link toLink = new FakeLink(Id.create(5, Link.class), null, null);
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(fromLink.getId(), toLink.getId());
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stopFacility1 = new TransitStopFacilityImpl(Id.create(1, TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
		TransitStopFacility stopFacility2 = new TransitStopFacilityImpl(Id.create(2, TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
		TransitStopFacility stopFacility3 = new TransitStopFacilityImpl(Id.create(3, TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
		TransitStopFacility stopFacility4 = new TransitStopFacilityImpl(Id.create(4, TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
		TransitRouteStop stop1 = new TransitRouteStopImpl.Builder().stop(stopFacility1).arrivalOffset(50).departureOffset(60).build();
		TransitRouteStop stop2 = new TransitRouteStopImpl.Builder().stop(stopFacility2).arrivalOffset(150).departureOffset(260).build();
		TransitRouteStop stop3 = new TransitRouteStopImpl.Builder().stop(stopFacility3).arrivalOffset(250).departureOffset(260).build();
		stops.add(stop1);
		stops.add(stop2);
		stops.add(stop3);

		TransitRoute tRoute = createTransitRoute(id, route, stops, "train");
		assertEquals(stops.size(), tRoute.getStops().size());

		// test order
		assertEquals(stop1, tRoute.getStops().get(0));
		assertEquals(stop2, tRoute.getStops().get(1));
		assertEquals(stop3, tRoute.getStops().get(2));

		// test getStop(TransitStopFacility)
		assertEquals(stop1, tRoute.getStop(stopFacility1));
		assertEquals(stop2, tRoute.getStop(stopFacility2));
		assertEquals(stop3, tRoute.getStop(stopFacility3));
		assertNull(tRoute.getStop(stopFacility4));
	}

	@Test
	void testGetStopsImmutable() {
		Fixture f = new Fixture();
		// test default of Fixture
		assertEquals(1, f.tRoute.getStops().size());
		try {
			f.tRoute.getStops().remove(0);
			fail("missing exception.");
		} catch (UnsupportedOperationException e) {
			log.info("catched expected exception. ", e);
		}
	}

	/**
	 * Helper class to minimize work needed to initialize a transit route object
	 *
	 * @author mrieser
	 */
	protected static class Fixture {

		public final TransitRoute tRoute;
		public final List<TransitRouteStop> stops;

		protected Fixture() {
			Id<TransitRoute> id = Id.create(9791, TransitRoute.class);
			Link fromLink = new FakeLink(Id.create(10, Link.class), null, null);
			Link toLink = new FakeLink(Id.create(5, Link.class), null, null);
			NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(fromLink.getId(), toLink.getId());
			this.stops = new ArrayList<TransitRouteStop>();
			TransitRouteStop stop = new TransitRouteStopImpl.Builder().arrivalOffset(50).departureOffset(60).build();
			this.stops.add(stop);

			this.tRoute = TransitRouteTest.createTransitRoute(id, route, this.stops, "train");
		}
	}
}
