/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleBuilderTest.java
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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class TransitScheduleFactoryTest {

	protected TransitScheduleFactory createTransitScheduleBuilder() {
		return new TransitScheduleFactoryImpl();
	}

	@Test
	void testCreateTransitSchedule() {
		TransitScheduleFactory builder = createTransitScheduleBuilder();
		TransitSchedule schedule = builder.createTransitSchedule();
		Assertions.assertEquals(builder, schedule.getFactory());
	}

	@Test
	void testCreateTransitLine() {
		TransitScheduleFactory builder = createTransitScheduleBuilder();
		Id<TransitLine> id = Id.create(1, TransitLine.class);
		TransitLine line = builder.createTransitLine(id);
		Assertions.assertEquals(id, line.getId());
	}

	@Test
	void testCreateTransitRoute() {
		TransitScheduleFactory builder = createTransitScheduleBuilder();
		Id<TransitRoute> id = Id.create(2, TransitRoute.class);
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(Id.create(3, Link.class), Id.create(4, Link.class));
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitRouteStop stop1 = new TransitRouteStopImpl.Builder().arrivalOffset(50).departureOffset(60).build();
		stops.add(stop1);
		String mode = TransportMode.pt;
		TransitRoute tRoute = builder.createTransitRoute(id, route, stops, mode);
		Assertions.assertEquals(id, tRoute.getId());
		Assertions.assertEquals(route, tRoute.getRoute());
		Assertions.assertEquals(1, tRoute.getStops().size());
		Assertions.assertEquals(stop1, tRoute.getStops().get(0));
		Assertions.assertEquals(mode, tRoute.getTransportMode());
	}

	@Test
	void testCreateTransitRouteStop() {
		TransitScheduleFactory builder = createTransitScheduleBuilder();
		TransitStopFacility stopFacility = new TransitStopFacilityImpl(Id.create(5, TransitStopFacility.class), new Coord((double) 6, (double) 6), false);
		double arrivalOffset = 23;
		double departureOffset = 42;
		TransitRouteStop stop = builder.createTransitRouteStop(stopFacility, 23, 42);
		Assertions.assertEquals(stopFacility, stop.getStopFacility());
		Assertions.assertEquals(arrivalOffset, stop.getArrivalOffset().seconds(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(departureOffset, stop.getDepartureOffset().seconds(), MatsimTestUtils.EPSILON);
	}

	@Test
	void testCreateTransitStopFacility() {
		TransitScheduleFactory builder = createTransitScheduleBuilder();
		Id<TransitStopFacility> id1 = Id.create(6, TransitStopFacility.class);
		Coord coord1 = new Coord((double) 511, (double) 1980);
		Id<TransitStopFacility> id2 = Id.create(7, TransitStopFacility.class);
		Coord coord2 = new Coord((double) 105, (double) 1979);
		TransitStopFacility stopFacility1 = builder.createTransitStopFacility(id1, coord1, false);
		Assertions.assertEquals(id1, stopFacility1.getId());
		Assertions.assertEquals(coord1.getX(), stopFacility1.getCoord().getX(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(coord1.getY(), stopFacility1.getCoord().getY(), MatsimTestUtils.EPSILON);
		Assertions.assertFalse(stopFacility1.getIsBlockingLane());
		TransitStopFacility stopFacility2 = builder.createTransitStopFacility(id2, coord2, true);
		Assertions.assertEquals(id2, stopFacility2.getId());
		Assertions.assertEquals(coord2.getX(), stopFacility2.getCoord().getX(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(coord2.getY(), stopFacility2.getCoord().getY(), MatsimTestUtils.EPSILON);
		Assertions.assertTrue(stopFacility2.getIsBlockingLane());
	}

	@Test
	void testCreateDeparture() {
		TransitScheduleFactory builder = createTransitScheduleBuilder();
		Id<Departure> id = Id.create(8, Departure.class);
		double time = 9.0*3600;
		Departure dep = builder.createDeparture(id, time);
		Assertions.assertEquals(id, dep.getId());
		Assertions.assertEquals(time, dep.getDepartureTime(), MatsimTestUtils.EPSILON);
	}

}
