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

package org.matsim.transitSchedule;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.matsim.transitSchedule.api.TransitStopFacility;

/**
 * @author mrieser
 */
public class TransitScheduleFactoryTest {

	protected TransitScheduleFactory createTransitScheduleBuilder() {
		return new TransitScheduleFactoryImpl();
	}

	@Test
	public void testCreateTransitSchedule() {
		TransitScheduleFactory builder = createTransitScheduleBuilder();
		TransitSchedule schedule = builder.createTransitSchedule();
		Assert.assertEquals(builder, schedule.getFactory());
	}

	@Test
	public void testCreateTransitLine() {
		TransitScheduleFactory builder = createTransitScheduleBuilder();
		Id id = new IdImpl(1);
		TransitLine line = builder.createTransitLine(id);
		Assert.assertEquals(id, line.getId());
	}

	@Test
	public void testCreateTransitRoute() {
		TransitScheduleFactory builder = createTransitScheduleBuilder();
		Id id = new IdImpl(2);
		NetworkRoute route = new LinkNetworkRouteImpl(new IdImpl(3), new IdImpl(4));
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		TransitRouteStop stop1 = new TransitRouteStopImpl(null, 50, 60);
		stops.add(stop1);
		TransportMode mode = TransportMode.pt;
		TransitRoute tRoute = builder.createTransitRoute(id, route, stops, mode);
		Assert.assertEquals(id, tRoute.getId());
		Assert.assertEquals(route, tRoute.getRoute());
		Assert.assertEquals(1, tRoute.getStops().size());
		Assert.assertEquals(stop1, tRoute.getStops().get(0));
		Assert.assertEquals(mode, tRoute.getTransportMode());
	}

	@Test
	public void testCreateTransitRouteStop() {
		TransitScheduleFactory builder = createTransitScheduleBuilder();
		TransitStopFacility stopFacility = new TransitStopFacilityImpl(new IdImpl(5), new CoordImpl(6, 6), false);
		double arrivalOffset = 23;
		double departureOffset = 42;
		TransitRouteStop stop = builder.createTransitRouteStop(stopFacility, 23, 42);
		Assert.assertEquals(stopFacility, stop.getStopFacility());
		Assert.assertEquals(arrivalOffset, stop.getArrivalOffset(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(departureOffset, stop.getDepartureOffset(), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testCreateTransitStopFacility() {
		TransitScheduleFactory builder = createTransitScheduleBuilder();
		Id id1 = new IdImpl(6);
		Coord coord1 = new CoordImpl(511, 1980);
		Id id2 = new IdImpl(7);
		Coord coord2 = new CoordImpl(105, 1979);
		TransitStopFacility stopFacility1 = builder.createTransitStopFacility(id1, coord1, false);
		Assert.assertEquals(id1, stopFacility1.getId());
		Assert.assertEquals(coord1.getX(), stopFacility1.getCoord().getX(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(coord1.getY(), stopFacility1.getCoord().getY(), MatsimTestUtils.EPSILON);
		Assert.assertFalse(stopFacility1.getIsBlockingLane());
		TransitStopFacility stopFacility2 = builder.createTransitStopFacility(id2, coord2, true);
		Assert.assertEquals(id2, stopFacility2.getId());
		Assert.assertEquals(coord2.getX(), stopFacility2.getCoord().getX(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(coord2.getY(), stopFacility2.getCoord().getY(), MatsimTestUtils.EPSILON);
		Assert.assertTrue(stopFacility2.getIsBlockingLane());
	}

	@Test
	public void testCreateDeparture() {
		TransitScheduleFactory builder = createTransitScheduleBuilder();
		Id id = new IdImpl(8);
		double time = 9.0*3600;
		Departure dep = builder.createDeparture(id, time);
		Assert.assertEquals(id, dep.getId());
		Assert.assertEquals(time, dep.getDepartureTime(), MatsimTestUtils.EPSILON);
	}

}
