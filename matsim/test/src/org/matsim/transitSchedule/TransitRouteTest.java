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

package org.matsim.transitSchedule;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.population.routes.NodeNetworkRouteImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.fakes.FakeLink;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitStopFacility;

/**
 * @author mrieser
 */
public class TransitRouteTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(TransitRouteTest.class);

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
	protected TransitRoute createTransitRoute(final Id id, final NetworkRouteWRefs route, final List<TransitRouteStop> stops, final TransportMode mode) {
		return new TransitRouteImpl(id, route, stops, mode);
	}

	public void testInitialization() {
		Id id = new IdImpl(9791);
		Link fromLink = new FakeLink(new IdImpl(10), null, null);
		Link toLink = new FakeLink(new IdImpl(5), null, null);
		NetworkRouteWRefs route = new NodeNetworkRouteImpl(fromLink, toLink);
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitRouteStop stop = new TransitRouteStopImpl(null, 50, 60);
		stops.add(stop);

		TransitRoute tRoute = createTransitRoute(id, route, stops, TransportMode.train);
		assertEquals("wrong id.", id.toString(), tRoute.getId().toString());
		assertEquals("wrong route.", route, tRoute.getRoute());
		assertEquals(stops.size(), tRoute.getStops().size());
		assertEquals(stop, tRoute.getStops().get(0));
		assertEquals(TransportMode.train, tRoute.getTransportMode());
	}

	public void testDescription() {
		Fixture f = new Fixture();
		assertNull(f.tRoute.getDescription());
		String desc = "some random description string.";
		f.tRoute.setDescription(desc);
		assertEquals(desc, f.tRoute.getDescription());
		desc += " [updated]";
		f.tRoute.setDescription(desc);
		assertEquals(desc, f.tRoute.getDescription());
	}

	public void testTransportMode() {
		Fixture f = new Fixture();
		// test default of Fixture
		assertEquals(TransportMode.train, f.tRoute.getTransportMode());
		// test some different from default
		f.tRoute.setTransportMode(TransportMode.tram);
		assertEquals(TransportMode.tram, f.tRoute.getTransportMode());
	}

	public void testAddDepartures() {
		Fixture f = new Fixture();
		Departure dep1 = new DepartureImpl(new IdImpl(1), 7.0*3600);
		Departure dep2 = new DepartureImpl(new IdImpl(2), 8.0*3600);
		Departure dep3 = new DepartureImpl(new IdImpl(3), 9.0*3600);
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

	public void testAddDeparturesException() {
		Fixture f = new Fixture();
		Departure dep1a = new DepartureImpl(new IdImpl(1), 7.0*3600);
		Departure dep1b = new DepartureImpl(new IdImpl(1), 7.0*3600);
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

	public void testRemoveDepartures() {
		Fixture f = new Fixture();
		Departure dep1 = new DepartureImpl(new IdImpl(1), 7.0*3600);
		Departure dep2 = new DepartureImpl(new IdImpl(2), 8.0*3600);

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

	public void testGetDeparturesImmutable() {
		Fixture f = new Fixture();
		Departure dep1 = new DepartureImpl(new IdImpl(1), 7.0*3600);
		assertEquals(0, f.tRoute.getDepartures().size());
		try {
			f.tRoute.getDepartures().put(dep1.getId(), dep1);
			fail("missing exception");
		}
		catch (UnsupportedOperationException e) {
			log.info("catched expected exception.", e);
		}
	}

	public void testRoute() {
		Fixture f = new Fixture();
		Link link1 = new FakeLink(new IdImpl(1), null, null);
		Link link2 = new FakeLink(new IdImpl(2), null, null);
		Link link3 = new FakeLink(new IdImpl(3), null, null);
		NetworkRouteWRefs route1 = new NodeNetworkRouteImpl(link1, link2);
		NetworkRouteWRefs route2 = new NodeNetworkRouteImpl(link1, link3);

		f.tRoute.setRoute(route1);
		assertEquals(route1, f.tRoute.getRoute());
		f.tRoute.setRoute(route2);
		assertEquals(route2, f.tRoute.getRoute());
	}

	public void testStops() {
		Id id = new IdImpl(9791);
		Link fromLink = new FakeLink(new IdImpl(10), null, null);
		Link toLink = new FakeLink(new IdImpl(5), null, null);
		NetworkRouteWRefs route = new NodeNetworkRouteImpl(fromLink, toLink);
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitStopFacility stopFacility1 = new TransitStopFacilityImpl(new IdImpl(1), new CoordImpl(0, 0), false);
		TransitStopFacility stopFacility2 = new TransitStopFacilityImpl(new IdImpl(2), new CoordImpl(0, 0), false);
		TransitStopFacility stopFacility3 = new TransitStopFacilityImpl(new IdImpl(3), new CoordImpl(0, 0), false);
		TransitStopFacility stopFacility4 = new TransitStopFacilityImpl(new IdImpl(4), new CoordImpl(0, 0), false);
		TransitRouteStop stop1 = new TransitRouteStopImpl(stopFacility1, 50, 60);
		TransitRouteStop stop2 = new TransitRouteStopImpl(stopFacility2, 150, 260);
		TransitRouteStop stop3 = new TransitRouteStopImpl(stopFacility3, 250, 260);
		stops.add(stop1);
		stops.add(stop2);
		stops.add(stop3);

		TransitRoute tRoute = createTransitRoute(id, route, stops, TransportMode.train);
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

	public void testGetStopsImmutable() {
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
	protected class Fixture {

		public final TransitRoute tRoute;
		public final List<TransitRouteStop> stops;

		protected Fixture() {
			Id id = new IdImpl(9791);
			Link fromLink = new FakeLink(new IdImpl(10), null, null);
			Link toLink = new FakeLink(new IdImpl(5), null, null);
			NetworkRouteWRefs route = new NodeNetworkRouteImpl(fromLink, toLink);
			this.stops = new ArrayList<TransitRouteStop>();
			TransitRouteStop stop = new TransitRouteStopImpl(null, 50, 60);
			this.stops.add(stop);

			this.tRoute = createTransitRoute(id, route, this.stops, TransportMode.train);
		}
	}
}
