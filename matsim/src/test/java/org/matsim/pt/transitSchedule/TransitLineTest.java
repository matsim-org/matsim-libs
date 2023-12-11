/* *********************************************************************** *
 * project: org.matsim.*
 * TransitLineTest.java
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class TransitLineTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	private static final Logger log = LogManager.getLogger(TransitLineTest.class);

	/**
	 * In case we once should have more than one implementation of
	 * {@link TransitLine}, simply inherit from this test and overwrite
	 * this method to return your own implementation.
	 *
	 * @param id
	 * @return a new instance of a TransitLine with the given Id set
	 */
	protected TransitLine createTransitLine(final Id<TransitLine> id) {
		return new TransitLineImpl(id);
	}

	@Test
	void testInitialization() {
		Id<TransitLine> id = Id.create(511, TransitLine.class);
		TransitLine tLine = createTransitLine(id);
		assertNotNull(tLine);
		assertEquals(id.toString(), tLine.getId().toString(), "different ids.");
	}

	@Test
	void testAddRoute() {
		TransitLine tLine = createTransitLine(Id.create("0891", TransitLine.class));
		TransitRoute route1 = new TransitRouteImpl(Id.create("1", TransitRoute.class), null, new ArrayList<TransitRouteStop>(), "bus");
		TransitRoute route2 = new TransitRouteImpl(Id.create("2", TransitRoute.class), null, new ArrayList<TransitRouteStop>(), "bus");
		assertEquals(0, tLine.getRoutes().size());
		tLine.addRoute(route1);
		assertEquals(1, tLine.getRoutes().size());
		assertNotNull(tLine.getRoutes().get(route1.getId()));
		tLine.addRoute(route2);
		assertEquals(2, tLine.getRoutes().size());
		assertNotNull(tLine.getRoutes().get(route1.getId()));
		assertNotNull(tLine.getRoutes().get(route2.getId()));
	}

	@Test
	void testAddRouteException() {
		TransitLine tLine = createTransitLine(Id.create("0891", TransitLine.class));
		TransitRoute route1a = new TransitRouteImpl(Id.create("1", TransitRoute.class), null, new ArrayList<TransitRouteStop>(), "bus");
		TransitRoute route1b = new TransitRouteImpl(Id.create("1", TransitRoute.class), null, new ArrayList<TransitRouteStop>(), "bus");
		assertEquals(0, tLine.getRoutes().size());
		tLine.addRoute(route1a);
		assertEquals(1, tLine.getRoutes().size());
		// try adding a route with same id
		try {
			tLine.addRoute(route1b);
			fail("missing exception.");
		}
		catch (IllegalArgumentException e) {
			log.info("catched expected exception.", e);
		}
		// try adding a route a second time
		try {
			tLine.addRoute(route1a);
			fail("missing excetion.");
		}
		catch (IllegalArgumentException e) {
			log.info("catched expected exception. ", e);
		}

	}

	@Test
	void testRemoveRoute() {
		TransitLine tLine = createTransitLine(Id.create("1980", TransitLine.class));
		TransitRoute route1 = new TransitRouteImpl(Id.create("11", TransitRoute.class), null, new ArrayList<TransitRouteStop>(), "bus");
		TransitRoute route2 = new TransitRouteImpl(Id.create("5", TransitRoute.class), null, new ArrayList<TransitRouteStop>(), "bus");
		assertEquals(0, tLine.getRoutes().size());

		tLine.addRoute(route1);
		tLine.addRoute(route2);
		assertEquals(2, tLine.getRoutes().size());
		assertNotNull(tLine.getRoutes().get(route1.getId()));
		assertNotNull(tLine.getRoutes().get(route2.getId()));

		assertTrue(tLine.removeRoute(route1));
		assertEquals(1, tLine.getRoutes().size());
		assertNull(tLine.getRoutes().get(route1.getId()));
		assertNotNull(tLine.getRoutes().get(route2.getId()));

		assertTrue(tLine.removeRoute(route2));
		assertEquals(0, tLine.getRoutes().size());

		tLine.addRoute(route1);
		assertEquals(1, tLine.getRoutes().size());
		assertFalse(tLine.removeRoute(route2));
		assertEquals(1, tLine.getRoutes().size());
		assertNotNull(tLine.getRoutes().get(route1.getId()));
	}

	@Test
	void testGetRoutesImmutable() {
		TransitLine tLine = createTransitLine(Id.create("1980", TransitLine.class));
		TransitRoute route1 = new TransitRouteImpl(Id.create("11", TransitRoute.class), null, new ArrayList<TransitRouteStop>(), "bus");
		try {
			tLine.getRoutes().put(route1.getId(), route1);
			fail("missing exception");
		}
		catch (UnsupportedOperationException e) {
			log.info("catched expected exception.", e);
		}
	}

}
