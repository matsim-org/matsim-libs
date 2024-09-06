/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleReaderV1Test.java
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

import java.util.Stack;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.AttributesBuilder;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.Attributes;

/**
 * Detailed tests for {@link TransitScheduleReaderV1}, also testing special cases
 * and possible Exceptions.
 *
 * @author mrieser
 */
public class TransitScheduleReaderV1Test {

	private static final String EMPTY_STRING = "";

	@Test
	void testStopFacility_Minimalistic() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		Attributes emptyAtts = AttributesBuilder.getEmpty();
		reader.startTag(Constants.TRANSIT_SCHEDULE, emptyAtts, context);
		context.push(Constants.TRANSIT_SCHEDULE);
		reader.startTag(Constants.TRANSIT_STOPS, emptyAtts, context);
		context.push(Constants.TRANSIT_STOPS);
		Attributes atts = new AttributesBuilder().add(Constants.ID, "stop1").
				add(Constants.X, "79").add(Constants.Y, "80").get();
		reader.startTag(Constants.STOP_FACILITY, atts, context);
		reader.endTag(Constants.STOP_FACILITY, EMPTY_STRING, context);
		reader.endTag(context.pop(), EMPTY_STRING, context);// TRANSIT_STOPS
		reader.endTag(context.pop(), EMPTY_STRING, context);// TRANSIT_SCHEDULE

		assertEquals(1, schedule.getFacilities().size());
		TransitStopFacility stop = schedule.getFacilities().get(Id.create("stop1", TransitStopFacility.class));
		assertNotNull(stop);
		assertEquals(79.0, stop.getCoord().getX(), MatsimTestUtils.EPSILON);
		assertEquals(80.0, stop.getCoord().getY(), MatsimTestUtils.EPSILON);
		assertNull(stop.getLinkId());
		assertNull(stop.getName());
		assertFalse(stop.getIsBlockingLane());
	}

	@Test
	void testStopFacility_withLink() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();
        Network network = NetworkUtils.createNetwork();
        Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 10, (double) 5));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 5, (double) 11));
		final Node fromNode = node1;
		final Node toNode = node2;
		Link link3 = NetworkUtils.createAndAddLink(network,Id.create(3, Link.class), fromNode, toNode, (double) 1000, 10.0, 2000.0, 1.0 );

		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		Attributes emptyAtts = AttributesBuilder.getEmpty();
		reader.startTag(Constants.TRANSIT_SCHEDULE, emptyAtts, context);
		context.push(Constants.TRANSIT_SCHEDULE);
		reader.startTag(Constants.TRANSIT_STOPS, emptyAtts, context);
		context.push(Constants.TRANSIT_STOPS);
		Attributes atts = new AttributesBuilder().add(Constants.ID, "stop1").
				add(Constants.X, "79").add(Constants.Y, "80").add(Constants.LINK_REF_ID, "3").get();
		reader.startTag(Constants.STOP_FACILITY, atts, context);
		reader.endTag(Constants.STOP_FACILITY, EMPTY_STRING, context);
		reader.endTag(context.pop(), EMPTY_STRING, context);// TRANSIT_STOPS
		reader.endTag(context.pop(), EMPTY_STRING, context);// TRANSIT_SCHEDULE

		assertEquals(1, schedule.getFacilities().size());
		TransitStopFacility stop = schedule.getFacilities().get(Id.create("stop1", TransitStopFacility.class));
		assertNotNull(stop);
		assertEquals(79.0, stop.getCoord().getX(), MatsimTestUtils.EPSILON);
		assertEquals(80.0, stop.getCoord().getY(), MatsimTestUtils.EPSILON);
		assertEquals(link3.getId(), stop.getLinkId());
	}

	@Test
	void testStopFacility_withBadLink() {
        TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();
        Network network = NetworkUtils.createNetwork();
        Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 10, (double) 5));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 5, (double) 11));
		final Node fromNode = node1;
		final Node toNode = node2;
		NetworkUtils.createAndAddLink(network,Id.create(3, Link.class), fromNode, toNode, (double) 1000, 10.0, 2000.0, 1.0 );

		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		Attributes emptyAtts = AttributesBuilder.getEmpty();
		reader.startTag(Constants.TRANSIT_SCHEDULE, emptyAtts, context);
		context.push(Constants.TRANSIT_SCHEDULE);
		reader.startTag(Constants.TRANSIT_STOPS, emptyAtts, context);
		context.push(Constants.TRANSIT_STOPS);
		Attributes atts = new AttributesBuilder().add(Constants.ID, "stop1").
				add(Constants.X, "79").add(Constants.Y, "80").add(Constants.LINK_REF_ID, "4").get();
		reader.startTag(Constants.STOP_FACILITY, atts, context);
		reader.endTag(Constants.STOP_FACILITY, "", context);
		context.pop();
		reader.endTag(Constants.TRANSIT_STOPS, "", context);
		context.pop();
		reader.endTag(Constants.TRANSIT_SCHEDULE, "", context);

		TransitStopFacility stop = schedule.getFacilities().get(Id.create("stop1", TransitStopFacility.class));
		assertEquals(Id.create("4", Link.class), stop.getLinkId());
		assertNull(network.getLinks().get(stop.getLinkId()));
	}

	@Test
	void testStopFacility_withName() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		Attributes emptyAtts = AttributesBuilder.getEmpty();
		reader.startTag(Constants.TRANSIT_SCHEDULE, emptyAtts, context);
		context.push(Constants.TRANSIT_SCHEDULE);
		reader.startTag(Constants.TRANSIT_STOPS, emptyAtts, context);
		context.push(Constants.TRANSIT_STOPS);
		Attributes atts = new AttributesBuilder().add(Constants.ID, "stop1").
				add(Constants.X, "79").add(Constants.Y, "80").add(Constants.NAME, "some stop name").get();
		reader.startTag(Constants.STOP_FACILITY, atts, context);
		reader.endTag(Constants.STOP_FACILITY, EMPTY_STRING, context);
		reader.endTag(context.pop(), EMPTY_STRING, context);// TRANSIT_STOPS
		reader.endTag(context.pop(), EMPTY_STRING, context);// TRANSIT_SCHEDULE

		assertEquals(1, schedule.getFacilities().size());
		TransitStopFacility stop = schedule.getFacilities().get(Id.create("stop1", TransitStopFacility.class));
		assertNotNull(stop);
		assertEquals(79.0, stop.getCoord().getX(), MatsimTestUtils.EPSILON);
		assertEquals(80.0, stop.getCoord().getY(), MatsimTestUtils.EPSILON);
		assertEquals("some stop name", stop.getName());
	}

	@Test
	void testStopFacility_isBlocking() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();

		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		Attributes emptyAtts = AttributesBuilder.getEmpty();
		reader.startTag(Constants.TRANSIT_SCHEDULE, emptyAtts, context);
		context.push(Constants.TRANSIT_SCHEDULE);
		reader.startTag(Constants.TRANSIT_STOPS, emptyAtts, context);
		context.push(Constants.TRANSIT_STOPS);
		Attributes atts = new AttributesBuilder().add(Constants.ID, "stop1").
				add(Constants.X, "79").add(Constants.Y, "80").add(Constants.IS_BLOCKING, "true").get();
		reader.startTag(Constants.STOP_FACILITY, atts, context);
		reader.endTag(Constants.STOP_FACILITY, EMPTY_STRING, context);
		reader.endTag(context.pop(), EMPTY_STRING, context);// TRANSIT_STOPS
		reader.endTag(context.pop(), EMPTY_STRING, context);// TRANSIT_SCHEDULE

		assertEquals(1, schedule.getFacilities().size());
		TransitStopFacility stop = schedule.getFacilities().get(Id.create("stop1", TransitStopFacility.class));
		assertNotNull(stop);
		assertEquals(79.0, stop.getCoord().getX(), MatsimTestUtils.EPSILON);
		assertEquals(80.0, stop.getCoord().getY(), MatsimTestUtils.EPSILON);
		assertTrue(stop.getIsBlockingLane());
	}

	@Test
	void testStopFacility_Multiple() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		Attributes emptyAtts = AttributesBuilder.getEmpty();
		reader.startTag(Constants.TRANSIT_SCHEDULE, emptyAtts, context);
		context.push(Constants.TRANSIT_SCHEDULE);
		reader.startTag(Constants.TRANSIT_STOPS, emptyAtts, context);
		context.push(Constants.TRANSIT_STOPS);

		Attributes atts = new AttributesBuilder().add(Constants.ID, "stop1").
				add(Constants.X, "79").add(Constants.Y, "80").get();
		reader.startTag(Constants.STOP_FACILITY, atts, context);
		reader.endTag(Constants.STOP_FACILITY, EMPTY_STRING, context);

		atts = new AttributesBuilder().add(Constants.ID, "stop2").
		add(Constants.X, "51").add(Constants.Y, "42").get();
		reader.startTag(Constants.STOP_FACILITY, atts, context);
		reader.endTag(Constants.STOP_FACILITY, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context);// TRANSIT_STOPS
		reader.endTag(context.pop(), EMPTY_STRING, context);// TRANSIT_SCHEDULE

		assertEquals(2, schedule.getFacilities().size());
		TransitStopFacility stop1 = schedule.getFacilities().get(Id.create("stop1", TransitStopFacility.class));
		assertNotNull(stop1);
		assertEquals(79.0, stop1.getCoord().getX(), MatsimTestUtils.EPSILON);
		assertEquals(80.0, stop1.getCoord().getY(), MatsimTestUtils.EPSILON);
		TransitStopFacility stop2 = schedule.getFacilities().get(Id.create("stop2", TransitStopFacility.class));
		assertNotNull(stop2);
		assertEquals(51.0, stop2.getCoord().getX(), MatsimTestUtils.EPSILON);
		assertEquals(42.0, stop2.getCoord().getY(), MatsimTestUtils.EPSILON);
	}

	@Test
	void testTransitLine_Single() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		reader.startTag(Constants.TRANSIT_SCHEDULE, AttributesBuilder.getEmpty(), context);
		context.push(Constants.TRANSIT_SCHEDULE);
		Id<TransitLine> lineId = Id.create("23", TransitLine.class);
		reader.startTag(Constants.TRANSIT_LINE, new AttributesBuilder().add(Constants.ID, lineId.toString()).get(), context);
		reader.endTag(Constants.TRANSIT_LINE, EMPTY_STRING, context);// TRANSIT_LINE
		reader.endTag(context.pop(), EMPTY_STRING, context);// TRANSIT_SCHEDULE

		assertEquals(1, schedule.getTransitLines().size());
		TransitLine line = schedule.getTransitLines().get(lineId);
		assertNotNull(line);
		assertEquals(lineId, line.getId());
	}

	@Test
	void testTransitLine_Multiple() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		reader.startTag(Constants.TRANSIT_SCHEDULE, AttributesBuilder.getEmpty(), context);
		context.push(Constants.TRANSIT_SCHEDULE);
		Id<TransitLine> lineId1 = Id.create("23", TransitLine.class);
		reader.startTag(Constants.TRANSIT_LINE, new AttributesBuilder().add(Constants.ID, lineId1.toString()).get(), context);
		reader.endTag(Constants.TRANSIT_LINE, EMPTY_STRING, context);// TRANSIT_LINE
		Id<TransitLine> lineId2 = Id.create("42", TransitLine.class);
		reader.startTag(Constants.TRANSIT_LINE, new AttributesBuilder().add(Constants.ID, lineId2.toString()).get(), context);
		reader.endTag(Constants.TRANSIT_LINE, EMPTY_STRING, context);// TRANSIT_LINE
		reader.endTag(context.pop(), EMPTY_STRING, context);// TRANSIT_SCHEDULE

		assertEquals(2, schedule.getTransitLines().size());
		TransitLine line1 = schedule.getTransitLines().get(lineId1);
		assertNotNull(line1);
		assertEquals(lineId1, line1.getId());
		TransitLine line2 = schedule.getTransitLines().get(lineId2);
		assertNotNull(line2);
		assertEquals(lineId2, line2.getId());
	}

	@Test
	void testTransitRoute_Single() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		reader.startTag(Constants.TRANSIT_SCHEDULE, AttributesBuilder.getEmpty(), context);
		context.push(Constants.TRANSIT_SCHEDULE);
		Id<TransitLine> lineId = Id.create("1", TransitLine.class);
		reader.startTag(Constants.TRANSIT_LINE, new AttributesBuilder().add(Constants.ID, lineId.toString()).get(), context);
		context.push(Constants.TRANSIT_LINE);

		Id<TransitRoute> routeId1 = Id.create("foo", TransitRoute.class);
		reader.startTag(Constants.TRANSIT_ROUTE, new AttributesBuilder().add(Constants.ID, routeId1.toString()).get(), context);
		context.push(Constants.TRANSIT_ROUTE);

		// by definition of the file format, transitRoute *must* have transportMode, routeProfile and departures defined
		reader.startTag(Constants.TRANSPORT_MODE, AttributesBuilder.getEmpty(), context);
		reader.endTag(Constants.TRANSPORT_MODE, "bus", context);
		reader.startTag(Constants.ROUTE_PROFILE, AttributesBuilder.getEmpty(), context); // route profile can be empty
		reader.endTag(Constants.ROUTE_PROFILE, EMPTY_STRING, context);
		reader.startTag(Constants.DEPARTURES, AttributesBuilder.getEmpty(), context); // departures can be empty
		reader.endTag(Constants.DEPARTURES, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_ROUTE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_LINE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_SCHEDULE

		TransitLine line = schedule.getTransitLines().get(lineId);
		assertEquals(1, line.getRoutes().size());
		TransitRoute route1 = line.getRoutes().get(routeId1);
		assertNotNull(route1);
		assertEquals(routeId1, route1.getId());
		assertNull(route1.getDescription());
		assertEquals("bus", route1.getTransportMode());
		assertEquals(0, route1.getStops().size());
		assertNull(route1.getRoute());
		assertEquals(0, route1.getDepartures().size());
	}

	@Test
	void testTransitRoute_Multiple() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		reader.startTag(Constants.TRANSIT_SCHEDULE, AttributesBuilder.getEmpty(), context);
		context.push(Constants.TRANSIT_SCHEDULE);
		Id<TransitLine> lineId = Id.create("1", TransitLine.class);
		reader.startTag(Constants.TRANSIT_LINE, new AttributesBuilder().add(Constants.ID, lineId.toString()).get(), context);
		context.push(Constants.TRANSIT_LINE);

		Id<TransitRoute> routeId1 = Id.create("foo", TransitRoute.class);
		reader.startTag(Constants.TRANSIT_ROUTE, new AttributesBuilder().add(Constants.ID, routeId1.toString()).get(), context);
		context.push(Constants.TRANSIT_ROUTE);

		// by definition of the file format, transitRoute *must* have transportMode, routeProfile and departures defined
		reader.startTag(Constants.TRANSPORT_MODE, AttributesBuilder.getEmpty(), context);
		reader.endTag(Constants.TRANSPORT_MODE, "bus", context);
		reader.startTag(Constants.ROUTE_PROFILE, AttributesBuilder.getEmpty(), context); // route profile can be empty
		reader.endTag(Constants.ROUTE_PROFILE, EMPTY_STRING, context);
		reader.startTag(Constants.DEPARTURES, AttributesBuilder.getEmpty(), context); // departures can be empty
		reader.endTag(Constants.DEPARTURES, EMPTY_STRING, context);
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_ROUTE

		Id<TransitRoute> routeId2 = Id.create("bar", TransitRoute.class);
		reader.startTag(Constants.TRANSIT_ROUTE, new AttributesBuilder().add(Constants.ID, routeId2.toString()).get(), context);
		context.push(Constants.TRANSIT_ROUTE);

		// by definition of the file format, transitRoute *must* have transportMode, routeProfile and departures defined
		reader.startTag(Constants.TRANSPORT_MODE, AttributesBuilder.getEmpty(), context);
		reader.endTag(Constants.TRANSPORT_MODE, "train", context);
		reader.startTag(Constants.ROUTE_PROFILE, AttributesBuilder.getEmpty(), context); // route profile can be empty
		reader.endTag(Constants.ROUTE_PROFILE, EMPTY_STRING, context);
		reader.startTag(Constants.DEPARTURES, AttributesBuilder.getEmpty(), context); // departures can be empty
		reader.endTag(Constants.DEPARTURES, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_ROUTE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_LINE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_SCHEDULE

		TransitLine line = schedule.getTransitLines().get(lineId);
		assertEquals(2, line.getRoutes().size());

		TransitRoute route1 = line.getRoutes().get(routeId1);
		assertNotNull(route1);
		assertEquals(routeId1, route1.getId());
		assertEquals("bus", route1.getTransportMode());

		TransitRoute route2 = line.getRoutes().get(routeId2);
		assertNotNull(route2);
		assertEquals(routeId2, route2.getId());
		assertEquals("train", route2.getTransportMode());

	}

	@Test
	void testTransitRoute_Description() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		reader.startTag(Constants.TRANSIT_SCHEDULE, AttributesBuilder.getEmpty(), context);
		context.push(Constants.TRANSIT_SCHEDULE);
		Id<TransitLine> lineId = Id.create("1", TransitLine.class);
		reader.startTag(Constants.TRANSIT_LINE, new AttributesBuilder().add(Constants.ID, lineId.toString()).get(), context);
		context.push(Constants.TRANSIT_LINE);

		Id<TransitRoute> routeId1 = Id.create("foo", TransitRoute.class);
		reader.startTag(Constants.TRANSIT_ROUTE, new AttributesBuilder().add(Constants.ID, routeId1.toString()).get(), context);
		context.push(Constants.TRANSIT_ROUTE);

		String description = "This could be some really long text, even containing line\nbreaks\n\nand other\tspecial characters.";
		reader.startTag(Constants.DESCRIPTION, AttributesBuilder.getEmpty(), context);
		reader.endTag(Constants.DESCRIPTION, description, context);

		// by definition of the file format, transitRoute *must* have transportMode, routeProfile and departures defined
		reader.startTag(Constants.TRANSPORT_MODE, AttributesBuilder.getEmpty(), context);
		reader.endTag(Constants.TRANSPORT_MODE, "bus", context);
		reader.startTag(Constants.ROUTE_PROFILE, AttributesBuilder.getEmpty(), context); // route profile can be empty
		reader.endTag(Constants.ROUTE_PROFILE, EMPTY_STRING, context);
		reader.startTag(Constants.DEPARTURES, AttributesBuilder.getEmpty(), context); // departures can be empty
		reader.endTag(Constants.DEPARTURES, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_ROUTE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_LINE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_SCHEDULE

		TransitLine line = schedule.getTransitLines().get(lineId);
		assertEquals(1, line.getRoutes().size());
		TransitRoute route1 = line.getRoutes().get(routeId1);
		assertNotNull(route1);
		assertEquals(description, route1.getDescription());
	}

	@Test
	void testRouteProfile_SingleStop() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		reader.startTag(Constants.TRANSIT_SCHEDULE, AttributesBuilder.getEmpty(), context);
		context.push(Constants.TRANSIT_SCHEDULE);

		// define some transit stops
		reader.startTag(Constants.TRANSIT_STOPS, AttributesBuilder.getEmpty(), context);
		context.push(Constants.TRANSIT_STOPS);
		Id<TransitStopFacility> stopId = Id.create("stop1", TransitStopFacility.class);
		Attributes atts = new AttributesBuilder().add(Constants.ID, stopId.toString()).
				add(Constants.X, "79").add(Constants.Y, "80").get();
		reader.startTag(Constants.STOP_FACILITY, atts, context);
		reader.endTag(Constants.STOP_FACILITY, EMPTY_STRING, context);

		// now the other stuff
		Id<TransitLine> lineId = Id.create("1", TransitLine.class);
		reader.startTag(Constants.TRANSIT_LINE, new AttributesBuilder().add(Constants.ID, lineId.toString()).get(), context);
		context.push(Constants.TRANSIT_LINE);

		Id<TransitRoute> routeId1 = Id.create("1", TransitRoute.class);
		reader.startTag(Constants.TRANSIT_ROUTE, new AttributesBuilder().add(Constants.ID, routeId1.toString()).get(), context);
		context.push(Constants.TRANSIT_ROUTE);

		// by definition of the file format, transitRoute *must* have transportMode, routeProfile and departures defined
		reader.startTag(Constants.TRANSPORT_MODE, AttributesBuilder.getEmpty(), context);
		reader.endTag(Constants.TRANSPORT_MODE, "bus", context);
		reader.startTag(Constants.ROUTE_PROFILE, AttributesBuilder.getEmpty(), context); // route profile can be empty
		context.push(Constants.ROUTE_PROFILE);

		reader.startTag(Constants.STOP, new AttributesBuilder().add(Constants.REF_ID, "stop1").get(), context);
		reader.endTag(Constants.STOP, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // ROUTE_PROFILE
		reader.endTag(Constants.ROUTE_PROFILE, EMPTY_STRING, context);
		reader.startTag(Constants.DEPARTURES, AttributesBuilder.getEmpty(), context); // departures can be empty
		reader.endTag(Constants.DEPARTURES, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_ROUTE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_LINE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_SCHEDULE

		TransitRoute route = schedule.getTransitLines().get(lineId).getRoutes().get(routeId1);
		assertEquals(1, route.getStops().size());
		TransitRouteStop stop1 = route.getStops().get(0);
		assertNotNull(stop1);
		assertEquals(schedule.getFacilities().get(stopId), stop1.getStopFacility());
		assertTrue(stop1.getArrivalOffset().isUndefined());
		assertTrue(stop1.getDepartureOffset().isUndefined());
		assertFalse(stop1.isAwaitDepartureTime());
	}

	@Test
	void testRouteProfile_MultipleStop() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		reader.startTag(Constants.TRANSIT_SCHEDULE, AttributesBuilder.getEmpty(), context);
		context.push(Constants.TRANSIT_SCHEDULE);

		// define some transit stops
		reader.startTag(Constants.TRANSIT_STOPS, AttributesBuilder.getEmpty(), context);
		context.push(Constants.TRANSIT_STOPS);
		Id<TransitStopFacility> stopId1 = Id.create("stop1", TransitStopFacility.class);
		Attributes atts = new AttributesBuilder().add(Constants.ID, stopId1.toString()).
				add(Constants.X, "79").add(Constants.Y, "80").get();
		reader.startTag(Constants.STOP_FACILITY, atts, context);
		reader.endTag(Constants.STOP_FACILITY, EMPTY_STRING, context);
		Id<TransitStopFacility> stopId2 = Id.create("stop2", TransitStopFacility.class);
		atts = new AttributesBuilder().add(Constants.ID, stopId2.toString()).
				add(Constants.X, "51").add(Constants.Y, "42").get();
		reader.startTag(Constants.STOP_FACILITY, atts, context);
		reader.endTag(Constants.STOP_FACILITY, EMPTY_STRING, context);
		Id<TransitStopFacility> stopId3 = Id.create("stop3", TransitStopFacility.class);
		atts = new AttributesBuilder().add(Constants.ID, stopId3.toString()).
				add(Constants.X, "76").add(Constants.Y, "80").get();
		reader.startTag(Constants.STOP_FACILITY, atts, context);
		reader.endTag(Constants.STOP_FACILITY, EMPTY_STRING, context);

		// now the other stuff
		Id<TransitLine> lineId = Id.create("1", TransitLine.class);
		reader.startTag(Constants.TRANSIT_LINE, new AttributesBuilder().add(Constants.ID, lineId.toString()).get(), context);
		context.push(Constants.TRANSIT_LINE);

		Id<TransitRoute> routeId1 = Id.create("1", TransitRoute.class);
		reader.startTag(Constants.TRANSIT_ROUTE, new AttributesBuilder().add(Constants.ID, routeId1.toString()).get(), context);
		context.push(Constants.TRANSIT_ROUTE);

		// by definition of the file format, transitRoute *must* have transportMode, routeProfile and departures defined
		reader.startTag(Constants.TRANSPORT_MODE, AttributesBuilder.getEmpty(), context);
		reader.endTag(Constants.TRANSPORT_MODE, "bus", context);
		reader.startTag(Constants.ROUTE_PROFILE, AttributesBuilder.getEmpty(), context); // route profile can be empty
		context.push(Constants.ROUTE_PROFILE);

		reader.startTag(Constants.STOP, new AttributesBuilder().add(Constants.REF_ID, "stop1").get(), context);
		reader.endTag(Constants.STOP, EMPTY_STRING, context);
		reader.startTag(Constants.STOP, new AttributesBuilder().add(Constants.REF_ID, "stop2").get(), context);
		reader.endTag(Constants.STOP, EMPTY_STRING, context);
		reader.startTag(Constants.STOP, new AttributesBuilder().add(Constants.REF_ID, "stop3").get(), context);
		reader.endTag(Constants.STOP, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // ROUTE_PROFILE
		reader.endTag(Constants.ROUTE_PROFILE, EMPTY_STRING, context);
		reader.startTag(Constants.DEPARTURES, AttributesBuilder.getEmpty(), context); // departures can be empty
		reader.endTag(Constants.DEPARTURES, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_ROUTE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_LINE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_SCHEDULE

		TransitRoute route = schedule.getTransitLines().get(lineId).getRoutes().get(routeId1);
		assertEquals(3, route.getStops().size());
		TransitRouteStop stop1 = route.getStops().get(0);
		assertNotNull(stop1);
		assertEquals(schedule.getFacilities().get(stopId1), stop1.getStopFacility());
		TransitRouteStop stop2 = route.getStops().get(0);
		assertNotNull(stop2);
		assertEquals(schedule.getFacilities().get(stopId1), stop2.getStopFacility());
		TransitRouteStop stop3 = route.getStops().get(0);
		assertNotNull(stop3);
		assertEquals(schedule.getFacilities().get(stopId1), stop3.getStopFacility());
	}

	@Test
	void testRouteProfileStop_Offsets() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		reader.startTag(Constants.TRANSIT_SCHEDULE, AttributesBuilder.getEmpty(), context);
		context.push(Constants.TRANSIT_SCHEDULE);

		// define some transit stops
		reader.startTag(Constants.TRANSIT_STOPS, AttributesBuilder.getEmpty(), context);
		context.push(Constants.TRANSIT_STOPS);
		Id<TransitStopFacility> stopId1 = Id.create("stop1", TransitStopFacility.class);
		Attributes atts = new AttributesBuilder().add(Constants.ID, stopId1.toString()).
				add(Constants.X, "79").add(Constants.Y, "80").get();
		reader.startTag(Constants.STOP_FACILITY, atts, context);
		reader.endTag(Constants.STOP_FACILITY, EMPTY_STRING, context);
		Id<TransitStopFacility> stopId2 = Id.create("stop2", TransitStopFacility.class);
		atts = new AttributesBuilder().add(Constants.ID, stopId2.toString()).
				add(Constants.X, "51").add(Constants.Y, "42").get();
		reader.startTag(Constants.STOP_FACILITY, atts, context);
		reader.endTag(Constants.STOP_FACILITY, EMPTY_STRING, context);
		Id<TransitStopFacility> stopId3 = Id.create("stop3", TransitStopFacility.class);
		atts = new AttributesBuilder().add(Constants.ID, stopId3.toString()).
				add(Constants.X, "76").add(Constants.Y, "80").get();
		reader.startTag(Constants.STOP_FACILITY, atts, context);
		reader.endTag(Constants.STOP_FACILITY, EMPTY_STRING, context);
		Id<TransitStopFacility> stopId4 = Id.create("stop4", TransitStopFacility.class);
		atts = new AttributesBuilder().add(Constants.ID, stopId4.toString()).
		add(Constants.X, "5").add(Constants.Y, "11").get();
		reader.startTag(Constants.STOP_FACILITY, atts, context);
		reader.endTag(Constants.STOP_FACILITY, EMPTY_STRING, context);

		// now the other stuff
		Id<TransitLine> lineId = Id.create("1", TransitLine.class);
		reader.startTag(Constants.TRANSIT_LINE, new AttributesBuilder().add(Constants.ID, lineId.toString()).get(), context);
		context.push(Constants.TRANSIT_LINE);

		Id<TransitRoute> routeId1 = Id.create("1", TransitRoute.class);
		reader.startTag(Constants.TRANSIT_ROUTE, new AttributesBuilder().add(Constants.ID, routeId1.toString()).get(), context);
		context.push(Constants.TRANSIT_ROUTE);

		// by definition of the file format, transitRoute *must* have transportMode, routeProfile and departures defined
		reader.startTag(Constants.TRANSPORT_MODE, AttributesBuilder.getEmpty(), context);
		reader.endTag(Constants.TRANSPORT_MODE, "bus", context);
		reader.startTag(Constants.ROUTE_PROFILE, AttributesBuilder.getEmpty(), context); // route profile can be empty
		context.push(Constants.ROUTE_PROFILE);

		reader.startTag(Constants.STOP, new AttributesBuilder().add(Constants.REF_ID, "stop1").
				add(Constants.ARRIVAL_OFFSET, "60").get(), context);
		reader.endTag(Constants.STOP, EMPTY_STRING, context);
		reader.startTag(Constants.STOP, new AttributesBuilder().add(Constants.REF_ID, "stop2").
				add(Constants.DEPARTURE_OFFSET, "90").get(), context);
		reader.endTag(Constants.STOP, EMPTY_STRING, context);
		reader.startTag(Constants.STOP, new AttributesBuilder().add(Constants.REF_ID, "stop3").
				add(Constants.ARRIVAL_OFFSET, "120").add(Constants.DEPARTURE_OFFSET, "150").get(), context);
		reader.endTag(Constants.STOP, EMPTY_STRING, context);
		reader.startTag(Constants.STOP, new AttributesBuilder().add(Constants.REF_ID, "stop4").get(), context);
		reader.endTag(Constants.STOP, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // ROUTE_PROFILE
		reader.endTag(Constants.ROUTE_PROFILE, EMPTY_STRING, context);
		reader.startTag(Constants.DEPARTURES, AttributesBuilder.getEmpty(), context); // departures can be empty
		reader.endTag(Constants.DEPARTURES, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_ROUTE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_LINE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_SCHEDULE

		TransitRoute route = schedule.getTransitLines().get(lineId).getRoutes().get(routeId1);
		assertEquals(4, route.getStops().size());
		TransitRouteStop stop1 = route.getStops().get(0);

		assertNotNull(stop1);
		assertEquals(schedule.getFacilities().get(stopId1), stop1.getStopFacility());
		assertEquals(60.0, stop1.getArrivalOffset().seconds(), MatsimTestUtils.EPSILON);
		assertTrue(stop1.getDepartureOffset().isUndefined());

		TransitRouteStop stop2 = route.getStops().get(1);
		assertNotNull(stop2);
		assertEquals(schedule.getFacilities().get(stopId2), stop2.getStopFacility());
		assertTrue(stop2.getArrivalOffset().isUndefined());
		assertEquals(90.0, stop2.getDepartureOffset().seconds(), MatsimTestUtils.EPSILON);

		TransitRouteStop stop3 = route.getStops().get(2);
		assertNotNull(stop3);
		assertEquals(schedule.getFacilities().get(stopId3), stop3.getStopFacility());
		assertEquals(120.0, stop3.getArrivalOffset().seconds(), MatsimTestUtils.EPSILON);
		assertEquals(150.0, stop3.getDepartureOffset().seconds(), MatsimTestUtils.EPSILON);

		TransitRouteStop stop4 = route.getStops().get(3);
		assertNotNull(stop4);
		assertEquals(schedule.getFacilities().get(stopId4), stop4.getStopFacility());
		assertTrue(stop4.getArrivalOffset().isUndefined());
		assertTrue(stop4.getDepartureOffset().isUndefined());
	}

	@Test
	void testRouteProfileStop_AwaitDeparture() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		reader.startTag(Constants.TRANSIT_SCHEDULE, AttributesBuilder.getEmpty(), context);
		context.push(Constants.TRANSIT_SCHEDULE);

		// define some transit stops
		reader.startTag(Constants.TRANSIT_STOPS, AttributesBuilder.getEmpty(), context);
		context.push(Constants.TRANSIT_STOPS);
		Id<TransitStopFacility> stopId1 = Id.create("stop1", TransitStopFacility.class);
		Attributes atts = new AttributesBuilder().add(Constants.ID, stopId1.toString()).
				add(Constants.X, "79").add(Constants.Y, "80").get();
		reader.startTag(Constants.STOP_FACILITY, atts, context);
		reader.endTag(Constants.STOP_FACILITY, EMPTY_STRING, context);
		Id<TransitStopFacility> stopId2 = Id.create("stop2", TransitStopFacility.class);
		atts = new AttributesBuilder().add(Constants.ID, stopId2.toString()).
				add(Constants.X, "51").add(Constants.Y, "42").get();
		reader.startTag(Constants.STOP_FACILITY, atts, context);
		reader.endTag(Constants.STOP_FACILITY, EMPTY_STRING, context);
		Id<TransitStopFacility> stopId3 = Id.create("stop3", TransitStopFacility.class);
		atts = new AttributesBuilder().add(Constants.ID, stopId3.toString()).
				add(Constants.X, "76").add(Constants.Y, "80").get();
		reader.startTag(Constants.STOP_FACILITY, atts, context);
		reader.endTag(Constants.STOP_FACILITY, EMPTY_STRING, context);

		// now the other stuff
		Id<TransitLine> lineId = Id.create("1", TransitLine.class);
		reader.startTag(Constants.TRANSIT_LINE, new AttributesBuilder().add(Constants.ID, lineId.toString()).get(), context);
		context.push(Constants.TRANSIT_LINE);

		Id<TransitRoute> routeId1 = Id.create("1", TransitRoute.class);
		reader.startTag(Constants.TRANSIT_ROUTE, new AttributesBuilder().add(Constants.ID, routeId1.toString()).get(), context);
		context.push(Constants.TRANSIT_ROUTE);

		// by definition of the file format, transitRoute *must* have transportMode, routeProfile and departures defined
		reader.startTag(Constants.TRANSPORT_MODE, AttributesBuilder.getEmpty(), context);
		reader.endTag(Constants.TRANSPORT_MODE, "bus", context);
		reader.startTag(Constants.ROUTE_PROFILE, AttributesBuilder.getEmpty(), context); // route profile can be empty
		context.push(Constants.ROUTE_PROFILE);

		reader.startTag(Constants.STOP, new AttributesBuilder().add(Constants.REF_ID, "stop1").get(), context); // awaitDeparture not specified
		reader.endTag(Constants.STOP, EMPTY_STRING, context);
		reader.startTag(Constants.STOP, new AttributesBuilder().add(Constants.REF_ID, "stop2").
				add(Constants.AWAIT_DEPARTURE, "true").get(), context);
		reader.endTag(Constants.STOP, EMPTY_STRING, context);
		reader.startTag(Constants.STOP, new AttributesBuilder().add(Constants.REF_ID, "stop3").
				add(Constants.AWAIT_DEPARTURE, "false").get(), context);
		reader.endTag(Constants.STOP, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // ROUTE_PROFILE
		reader.endTag(Constants.ROUTE_PROFILE, EMPTY_STRING, context);
		reader.startTag(Constants.DEPARTURES, AttributesBuilder.getEmpty(), context); // departures can be empty
		reader.endTag(Constants.DEPARTURES, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_ROUTE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_LINE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_SCHEDULE

		TransitRoute route = schedule.getTransitLines().get(lineId).getRoutes().get(routeId1);
		assertEquals(3, route.getStops().size());
		TransitRouteStop stop1 = route.getStops().get(0);
		assertEquals(false, stop1.isAwaitDepartureTime());
		TransitRouteStop stop2 = route.getStops().get(1);
		assertEquals(true, stop2.isAwaitDepartureTime());
		TransitRouteStop stop3 = route.getStops().get(2);
		assertEquals(false, stop3.isAwaitDepartureTime());
	}

	@Test
	void testRouteProfileRoute_NoLink() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		reader.startTag(Constants.TRANSIT_SCHEDULE, AttributesBuilder.getEmpty(), context);
		context.push(Constants.TRANSIT_SCHEDULE);

		// now the other stuff
		Id<TransitLine> lineId = Id.create("1", TransitLine.class);
		reader.startTag(Constants.TRANSIT_LINE, new AttributesBuilder().add(Constants.ID, lineId.toString()).get(), context);
		context.push(Constants.TRANSIT_LINE);

		Id<TransitRoute> routeId1 = Id.create("1", TransitRoute.class);
		reader.startTag(Constants.TRANSIT_ROUTE, new AttributesBuilder().add(Constants.ID, routeId1.toString()).get(), context);
		context.push(Constants.TRANSIT_ROUTE);

		// by definition of the file format, transitRoute *must* have transportMode, routeProfile and departures defined
		reader.startTag(Constants.TRANSPORT_MODE, AttributesBuilder.getEmpty(), context);
		reader.endTag(Constants.TRANSPORT_MODE, "bus", context);
		reader.startTag(Constants.ROUTE_PROFILE, AttributesBuilder.getEmpty(), context); // route profile can be empty, but must exist
		reader.endTag(Constants.ROUTE_PROFILE, EMPTY_STRING, context);

		reader.startTag(Constants.ROUTE, AttributesBuilder.getEmpty(), context);
		reader.endTag(Constants.ROUTE, EMPTY_STRING, context);

		reader.endTag(Constants.ROUTE_PROFILE, EMPTY_STRING, context);
		reader.startTag(Constants.DEPARTURES, AttributesBuilder.getEmpty(), context); // departures can be empty, but must exist
		reader.endTag(Constants.DEPARTURES, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_ROUTE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_LINE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_SCHEDULE

		TransitRoute route = schedule.getTransitLines().get(lineId).getRoutes().get(routeId1);
		assertNull(route.getRoute());
	}

	@Test
	void testRouteProfileRoute_OneLink() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();

        Network network = NetworkUtils.createNetwork();
        Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 10, (double) 5));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 5, (double) 11));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create(3, Node.class), new Coord((double) 5, (double) 11));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create(4, Node.class), new Coord((double) 5, (double) 11));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create(5, Node.class), new Coord((double) 5, (double) 11));
		final Node fromNode = node1;
		final Node toNode = node2;
		/*Link link1 =*/ NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), fromNode, toNode, (double) 1000, 10.0, 2000.0, 1.0 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		Link link2 = NetworkUtils.createAndAddLink(network,Id.create(2, Link.class), fromNode1, toNode1, (double) 1000, 10.0, 2000.0, 1.0 );
		final Node fromNode2 = node3;
		final Node toNode2 = node4;
		/*Link link3 =*/NetworkUtils.createAndAddLink(network,Id.create(3, Link.class), fromNode2, toNode2, (double) 1000, 10.0, 2000.0, 1.0 );
		final Node fromNode3 = node4;
		final Node toNode3 = node5;
		/*Link link4 =*/NetworkUtils.createAndAddLink(network,Id.create(4, Link.class), fromNode3, toNode3, (double) 1000, 10.0, 2000.0, 1.0 );

		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		reader.startTag(Constants.TRANSIT_SCHEDULE, AttributesBuilder.getEmpty(), context);
		context.push(Constants.TRANSIT_SCHEDULE);

		// now the other stuff
		Id<TransitLine> lineId = Id.create("1", TransitLine.class);
		reader.startTag(Constants.TRANSIT_LINE, new AttributesBuilder().add(Constants.ID, lineId.toString()).get(), context);
		context.push(Constants.TRANSIT_LINE);

		Id<TransitRoute> routeId1 = Id.create("1", TransitRoute.class);
		reader.startTag(Constants.TRANSIT_ROUTE, new AttributesBuilder().add(Constants.ID, routeId1.toString()).get(), context);
		context.push(Constants.TRANSIT_ROUTE);

		// by definition of the file format, transitRoute *must* have transportMode, routeProfile and departures defined
		reader.startTag(Constants.TRANSPORT_MODE, AttributesBuilder.getEmpty(), context);
		reader.endTag(Constants.TRANSPORT_MODE, "bus", context);
		reader.startTag(Constants.ROUTE_PROFILE, AttributesBuilder.getEmpty(), context); // route profile can be empty
		reader.endTag(Constants.ROUTE_PROFILE, EMPTY_STRING, context);

		reader.startTag(Constants.ROUTE, AttributesBuilder.getEmpty(), context);
		context.push(Constants.ROUTE);

		reader.startTag(Constants.LINK, new AttributesBuilder().add(Constants.REF_ID, "2").get(), context);
		reader.endTag(Constants.LINK, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // ROUTE

		reader.endTag(Constants.ROUTE_PROFILE, EMPTY_STRING, context);
		reader.startTag(Constants.DEPARTURES, AttributesBuilder.getEmpty(), context); // departures can be empty
		reader.endTag(Constants.DEPARTURES, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_ROUTE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_LINE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_SCHEDULE

		TransitRoute route = schedule.getTransitLines().get(lineId).getRoutes().get(routeId1);
		NetworkRoute netRoute = route.getRoute();
		assertNotNull(netRoute);
		assertEquals(link2.getId(), netRoute.getStartLinkId());
		assertEquals(link2.getId(), netRoute.getEndLinkId());
		assertEquals(0, netRoute.getLinkIds().size());
	}

	@Test
	void testRouteProfileRoute_TwoLinks() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();

        Network network = NetworkUtils.createNetwork();
        Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 10, (double) 5));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 5, (double) 11));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create(3, Node.class), new Coord((double) 5, (double) 11));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create(4, Node.class), new Coord((double) 5, (double) 11));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create(5, Node.class), new Coord((double) 5, (double) 11));
		final Node fromNode = node1;
		final Node toNode = node2;
		/*Link link1 =*/NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), fromNode, toNode, (double) 1000, 10.0, 2000.0, 1.0 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		/*Link link2 =*/NetworkUtils.createAndAddLink(network,Id.create(2, Link.class), fromNode1, toNode1, (double) 1000, 10.0, 2000.0, 1.0 );
		final Node fromNode2 = node3;
		final Node toNode2 = node4;
		Link link3 = NetworkUtils.createAndAddLink(network,Id.create(3, Link.class), fromNode2, toNode2, (double) 1000, 10.0, 2000.0, 1.0 );
		final Node fromNode3 = node4;
		final Node toNode3 = node5;
		Link link4 = NetworkUtils.createAndAddLink(network,Id.create(4, Link.class), fromNode3, toNode3, (double) 1000, 10.0, 2000.0, 1.0 );

		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		reader.startTag(Constants.TRANSIT_SCHEDULE, AttributesBuilder.getEmpty(), context);
		context.push(Constants.TRANSIT_SCHEDULE);

		// now the other stuff
		Id<TransitLine> lineId = Id.create("1", TransitLine.class);
		reader.startTag(Constants.TRANSIT_LINE, new AttributesBuilder().add(Constants.ID, lineId.toString()).get(), context);
		context.push(Constants.TRANSIT_LINE);

		Id<TransitRoute> routeId1 = Id.create("1", TransitRoute.class);
		reader.startTag(Constants.TRANSIT_ROUTE, new AttributesBuilder().add(Constants.ID, routeId1.toString()).get(), context);
		context.push(Constants.TRANSIT_ROUTE);

		// by definition of the file format, transitRoute *must* have transportMode, routeProfile and departures defined
		reader.startTag(Constants.TRANSPORT_MODE, AttributesBuilder.getEmpty(), context);
		reader.endTag(Constants.TRANSPORT_MODE, "bus", context);
		reader.startTag(Constants.ROUTE_PROFILE, AttributesBuilder.getEmpty(), context); // route profile can be empty
		reader.endTag(Constants.ROUTE_PROFILE, EMPTY_STRING, context);

		reader.startTag(Constants.ROUTE, AttributesBuilder.getEmpty(), context);
		context.push(Constants.ROUTE);

		reader.startTag(Constants.LINK, new AttributesBuilder().add(Constants.REF_ID, "3").get(), context);
		reader.endTag(Constants.LINK, EMPTY_STRING, context);
		reader.startTag(Constants.LINK, new AttributesBuilder().add(Constants.REF_ID, "4").get(), context);
		reader.endTag(Constants.LINK, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // ROUTE

		reader.endTag(Constants.ROUTE_PROFILE, EMPTY_STRING, context);
		reader.startTag(Constants.DEPARTURES, AttributesBuilder.getEmpty(), context); // departures can be empty
		reader.endTag(Constants.DEPARTURES, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_ROUTE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_LINE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_SCHEDULE

		TransitRoute route = schedule.getTransitLines().get(lineId).getRoutes().get(routeId1);
		NetworkRoute netRoute = route.getRoute();
		assertNotNull(netRoute);
		assertEquals(link3.getId(), netRoute.getStartLinkId());
		assertEquals(link4.getId(), netRoute.getEndLinkId());
		assertEquals(0, netRoute.getLinkIds().size());
	}

	@Test
	void testRouteProfileRoute_MoreLinks() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();

        Network network = NetworkUtils.createNetwork();
        Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 10, (double) 5));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 5, (double) 11));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create(3, Node.class), new Coord((double) 5, (double) 11));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create(4, Node.class), new Coord((double) 5, (double) 11));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create(5, Node.class), new Coord((double) 5, (double) 11));
		final Node fromNode = node1;
		final Node toNode = node2;
		Link link1 = NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), fromNode, toNode, (double) 1000, 10.0, 2000.0, 1.0 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		Link link2 = NetworkUtils.createAndAddLink(network,Id.create(2, Link.class), fromNode1, toNode1, (double) 1000, 10.0, 2000.0, 1.0 );
		final Node fromNode2 = node3;
		final Node toNode2 = node4;
		Link link3 = NetworkUtils.createAndAddLink(network,Id.create(3, Link.class), fromNode2, toNode2, (double) 1000, 10.0, 2000.0, 1.0 );
		final Node fromNode3 = node4;
		final Node toNode3 = node5;
		Link link4 = NetworkUtils.createAndAddLink(network,Id.create(4, Link.class), fromNode3, toNode3, (double) 1000, 10.0, 2000.0, 1.0 );

		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		reader.startTag(Constants.TRANSIT_SCHEDULE, AttributesBuilder.getEmpty(), context);
		context.push(Constants.TRANSIT_SCHEDULE);

		// now the other stuff
		Id<TransitLine> lineId = Id.create("1", TransitLine.class);
		reader.startTag(Constants.TRANSIT_LINE, new AttributesBuilder().add(Constants.ID, lineId.toString()).get(), context);
		context.push(Constants.TRANSIT_LINE);

		Id<TransitRoute> routeId1 = Id.create("1", TransitRoute.class);
		reader.startTag(Constants.TRANSIT_ROUTE, new AttributesBuilder().add(Constants.ID, routeId1.toString()).get(), context);
		context.push(Constants.TRANSIT_ROUTE);

		// by definition of the file format, transitRoute *must* have transportMode, routeProfile and departures defined
		reader.startTag(Constants.TRANSPORT_MODE, AttributesBuilder.getEmpty(), context);
		reader.endTag(Constants.TRANSPORT_MODE, "bus", context);
		reader.startTag(Constants.ROUTE_PROFILE, AttributesBuilder.getEmpty(), context); // route profile can be empty
		reader.endTag(Constants.ROUTE_PROFILE, EMPTY_STRING, context);

		reader.startTag(Constants.ROUTE, AttributesBuilder.getEmpty(), context);
		context.push(Constants.ROUTE);

		reader.startTag(Constants.LINK, new AttributesBuilder().add(Constants.REF_ID, "1").get(), context);
		reader.endTag(Constants.LINK, EMPTY_STRING, context);
		reader.startTag(Constants.LINK, new AttributesBuilder().add(Constants.REF_ID, "2").get(), context);
		reader.endTag(Constants.LINK, EMPTY_STRING, context);
		reader.startTag(Constants.LINK, new AttributesBuilder().add(Constants.REF_ID, "3").get(), context);
		reader.endTag(Constants.LINK, EMPTY_STRING, context);
		reader.startTag(Constants.LINK, new AttributesBuilder().add(Constants.REF_ID, "4").get(), context);
		reader.endTag(Constants.LINK, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // ROUTE

		reader.endTag(Constants.ROUTE_PROFILE, EMPTY_STRING, context);
		reader.startTag(Constants.DEPARTURES, AttributesBuilder.getEmpty(), context); // departures can be empty
		reader.endTag(Constants.DEPARTURES, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_ROUTE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_LINE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_SCHEDULE

		TransitRoute route = schedule.getTransitLines().get(lineId).getRoutes().get(routeId1);
		NetworkRoute netRoute = route.getRoute();
		assertNotNull(netRoute);
		assertEquals(link1.getId(), netRoute.getStartLinkId());
		assertEquals(link4.getId(), netRoute.getEndLinkId());
		assertEquals(2, netRoute.getLinkIds().size());
		assertEquals(link2.getId(), netRoute.getLinkIds().get(0));
		assertEquals(link3.getId(), netRoute.getLinkIds().get(1));
	}

	@Test
	void testDepartures_Single() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		reader.startTag(Constants.TRANSIT_SCHEDULE, AttributesBuilder.getEmpty(), context);
		context.push(Constants.TRANSIT_SCHEDULE);
		Id<TransitLine> lineId = Id.create("1", TransitLine.class);
		reader.startTag(Constants.TRANSIT_LINE, new AttributesBuilder().add(Constants.ID, lineId.toString()).get(), context);
		context.push(Constants.TRANSIT_LINE);

		Id<TransitRoute> routeId1 = Id.create("foo", TransitRoute.class);
		reader.startTag(Constants.TRANSIT_ROUTE, new AttributesBuilder().add(Constants.ID, routeId1.toString()).get(), context);
		context.push(Constants.TRANSIT_ROUTE);

		// by definition of the file format, transitRoute *must* have transportMode, routeProfile and departures defined
		reader.startTag(Constants.TRANSPORT_MODE, AttributesBuilder.getEmpty(), context);
		reader.endTag(Constants.TRANSPORT_MODE, "bus", context);
		reader.startTag(Constants.ROUTE_PROFILE, AttributesBuilder.getEmpty(), context); // route profile can be empty
		reader.endTag(Constants.ROUTE_PROFILE, EMPTY_STRING, context);
		reader.startTag(Constants.DEPARTURES, AttributesBuilder.getEmpty(), context); // departures can be empty
		context.push(Constants.DEPARTURES);

		Id<Departure> depId1 = Id.create("23", Departure.class);
		String depTime1 = "07:35:42";
		reader.startTag(Constants.DEPARTURE, new AttributesBuilder().add(Constants.ID, depId1.toString()).
				add(Constants.DEPARTURE_TIME, depTime1).get(), context);
		reader.endTag(Constants.DEPARTURE, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // DEPATURES
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_ROUTE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_LINE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_SCHEDULE

		TransitRoute route = schedule.getTransitLines().get(lineId).getRoutes().get(routeId1);
		assertEquals(1, route.getDepartures().size());
		Departure dep1 = route.getDepartures().get(depId1);
		assertNotNull(dep1);
		assertEquals(depId1, dep1.getId());
		assertEquals(Time.parseTime(depTime1), dep1.getDepartureTime(), MatsimTestUtils.EPSILON);
	}

	@Test
	void testDepartures_Multiple() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		reader.startTag(Constants.TRANSIT_SCHEDULE, AttributesBuilder.getEmpty(), context);
		context.push(Constants.TRANSIT_SCHEDULE);
		Id<TransitLine> lineId = Id.create("1", TransitLine.class);
		reader.startTag(Constants.TRANSIT_LINE, new AttributesBuilder().add(Constants.ID, lineId.toString()).get(), context);
		context.push(Constants.TRANSIT_LINE);

		Id<TransitRoute> routeId1 = Id.create("foo", TransitRoute.class);
		reader.startTag(Constants.TRANSIT_ROUTE, new AttributesBuilder().add(Constants.ID, routeId1.toString()).get(), context);
		context.push(Constants.TRANSIT_ROUTE);

		// by definition of the file format, transitRoute *must* have transportMode, routeProfile and departures defined
		reader.startTag(Constants.TRANSPORT_MODE, AttributesBuilder.getEmpty(), context);
		reader.endTag(Constants.TRANSPORT_MODE, "bus", context);
		reader.startTag(Constants.ROUTE_PROFILE, AttributesBuilder.getEmpty(), context); // route profile can be empty
		reader.endTag(Constants.ROUTE_PROFILE, EMPTY_STRING, context);
		reader.startTag(Constants.DEPARTURES, AttributesBuilder.getEmpty(), context); // departures can be empty
		context.push(Constants.DEPARTURES);

		Id<Departure> depId1 = Id.create("23", Departure.class);
		String depTime1 = "07:35:42";
		reader.startTag(Constants.DEPARTURE, new AttributesBuilder().add(Constants.ID, depId1.toString()).
				add(Constants.DEPARTURE_TIME, depTime1).get(), context);
		reader.endTag(Constants.DEPARTURE, EMPTY_STRING, context);

		Id<Departure> depId2 = Id.create("42", Departure.class);
		String depTime2 = "07:35:42";
		reader.startTag(Constants.DEPARTURE, new AttributesBuilder().add(Constants.ID, depId2.toString()).
				add(Constants.DEPARTURE_TIME, depTime2).get(), context);
		reader.endTag(Constants.DEPARTURE, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // DEPATURES
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_ROUTE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_LINE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_SCHEDULE

		TransitRoute route = schedule.getTransitLines().get(lineId).getRoutes().get(routeId1);
		assertEquals(2, route.getDepartures().size());
		Departure dep1 = route.getDepartures().get(depId1);
		assertNotNull(dep1);
		assertEquals(depId1, dep1.getId());
		assertEquals(Time.parseTime(depTime1), dep1.getDepartureTime(), MatsimTestUtils.EPSILON);
		Departure dep2 = route.getDepartures().get(depId2);
		assertNotNull(dep2);
		assertEquals(depId2, dep2.getId());
		assertEquals(Time.parseTime(depTime2), dep2.getDepartureTime(), MatsimTestUtils.EPSILON);
	}

	@Test
	void testDepartures_withVehicleRef() {
		TransitSchedule schedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(schedule, new RouteFactories());
		Stack<String> context = new Stack<>();
		reader.startTag(Constants.TRANSIT_SCHEDULE, AttributesBuilder.getEmpty(), context);
		context.push(Constants.TRANSIT_SCHEDULE);
		Id<TransitLine> lineId = Id.create("1", TransitLine.class);
		reader.startTag(Constants.TRANSIT_LINE, new AttributesBuilder().add(Constants.ID, lineId.toString()).get(), context);
		context.push(Constants.TRANSIT_LINE);

		Id<TransitRoute> routeId1 = Id.create("foo", TransitRoute.class);
		reader.startTag(Constants.TRANSIT_ROUTE, new AttributesBuilder().add(Constants.ID, routeId1.toString()).get(), context);
		context.push(Constants.TRANSIT_ROUTE);

		// by definition of the file format, transitRoute *must* have transportMode, routeProfile and departures defined
		reader.startTag(Constants.TRANSPORT_MODE, AttributesBuilder.getEmpty(), context);
		reader.endTag(Constants.TRANSPORT_MODE, "bus", context);
		reader.startTag(Constants.ROUTE_PROFILE, AttributesBuilder.getEmpty(), context); // route profile can be empty
		reader.endTag(Constants.ROUTE_PROFILE, EMPTY_STRING, context);
		reader.startTag(Constants.DEPARTURES, AttributesBuilder.getEmpty(), context); // departures can be empty
		context.push(Constants.DEPARTURES);

		Id<Departure> depId1 = Id.create("23", Departure.class);
		String depTime1 = "07:35:42";
		reader.startTag(Constants.DEPARTURE, new AttributesBuilder().add(Constants.ID, depId1.toString()).
				add(Constants.DEPARTURE_TIME, depTime1).add(Constants.VEHICLE_REF_ID, "v 975").get(), context);
		reader.endTag(Constants.DEPARTURE, EMPTY_STRING, context);

		Id<Departure> depId2 = Id.create("42", Departure.class);
		String depTime2 = "07:35:42";
		reader.startTag(Constants.DEPARTURE, new AttributesBuilder().add(Constants.ID, depId2.toString()).
				add(Constants.DEPARTURE_TIME, depTime2).get(), context);
		reader.endTag(Constants.DEPARTURE, EMPTY_STRING, context);

		reader.endTag(context.pop(), EMPTY_STRING, context); // DEPATURES
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_ROUTE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_LINE
		reader.endTag(context.pop(), EMPTY_STRING, context); // TRANSIT_SCHEDULE

		TransitRoute route = schedule.getTransitLines().get(lineId).getRoutes().get(routeId1);
		assertEquals(2, route.getDepartures().size());
		Departure dep1 = route.getDepartures().get(depId1);
		assertNotNull(dep1);
		assertEquals(depId1, dep1.getId());
		assertEquals(Time.parseTime(depTime1), dep1.getDepartureTime(), MatsimTestUtils.EPSILON);
		assertEquals(Id.create("v 975", Vehicle.class), dep1.getVehicleId());
		Departure dep2 = route.getDepartures().get(depId2);
		assertNotNull(dep2);
		assertEquals(depId2, dep2.getId());
		assertEquals(Time.parseTime(depTime2), dep2.getDepartureTime(), MatsimTestUtils.EPSILON);
		assertNull(dep2.getVehicleId());
	}


}
