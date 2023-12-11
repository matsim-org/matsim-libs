/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleWriterTest.java
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.SAXException;


/**
 * Tests the file format <code>transitSchedule_v1.dtd</code> by creating a
 * transit schedule with different attributes, writing it out to a file in
 * the specified file format, reads it back in and compares the actual values
 * with the previously set ones.
 *
 * @author mrieser
 */
public class TransitScheduleFormatV1Test {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testWriteRead() throws IOException, SAXException, ParserConfigurationException {
		// prepare required data
		Network network = NetworkUtils.createNetwork();
        Node n1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node n2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 0, (double) 0));
		Node n3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord((double) 0, (double) 0));
		Node n4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord((double) 0, (double) 0));
		Node n5 = NetworkUtils.createAndAddNode(network, Id.create("5", Node.class), new Coord((double) 0, (double) 0));
		final Node fromNode = n1;
		final Node toNode = n2;
		Link l1 = NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, (double) 1000, (double) 10, (double) 3600, 1.0 );
		final Node fromNode1 = n2;
		final Node toNode1 = n3;
		Link l2 = NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode1, toNode1, (double) 1000, (double) 10, (double) 3600, 1.0 );
		final Node fromNode2 = n3;
		final Node toNode2 = n4;
		Link l3 = NetworkUtils.createAndAddLink(network,Id.create("3", Link.class), fromNode2, toNode2, (double) 1000, (double) 10, (double) 3600, 1.0 );
		final Node fromNode3 = n4;
		final Node toNode3 = n5;
		Link l4 = NetworkUtils.createAndAddLink(network,Id.create("4", Link.class), fromNode3, toNode3, (double) 1000, (double) 10, (double) 3600, 1.0 );

		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitSchedule schedule1 = builder.createTransitSchedule();

		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create("stop1", TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create("stop2", TransitStopFacility.class), new Coord((double) 1000, (double) 0), true);
		TransitStopFacility stop3 = builder.createTransitStopFacility(Id.create("stop3", TransitStopFacility.class), new Coord((double) 1000, (double) 1000), true);
		TransitStopFacility stop4 = builder.createTransitStopFacility(Id.create("stop4", TransitStopFacility.class), new Coord((double) 0, (double) 1000), false);
		stop2.setName("S + U Nirgendwo");
		stop4.setName("Irgendwo");
		schedule1.addStopFacility(stop1);
		schedule1.addStopFacility(stop2);
		schedule1.addStopFacility(stop3);
		schedule1.addStopFacility(stop4);

		// prepare some schedule, without network-route
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>(4);
		stops.add(builder.createTransitRouteStopBuilder(stop1).departureOffset(0).build());
		stops.add(builder.createTransitRouteStop(stop2, 90, 120));
		TransitRouteStop routeStop3 = builder.createTransitRouteStopBuilder(stop3).departureOffset(300).build();
		routeStop3.setAwaitDepartureTime(true);
		stops.add(routeStop3);
		stops.add(builder.createTransitRouteStopBuilder(stop4).arrivalOffset(400).build());

		TransitRoute route1 = builder.createTransitRoute(Id.create(1, TransitRoute.class), null, stops, "bus");
		route1.setDescription("Just a comment.");

		route1.addDeparture(builder.createDeparture(Id.create("2", Departure.class), 7.0*3600));
		Departure dep = builder.createDeparture(Id.create("4", Departure.class), 7.0*3600 + 300);
		dep.setVehicleId(Id.create("86", Vehicle.class));
		route1.addDeparture(dep);
		dep = builder.createDeparture(Id.create("7", Departure.class), 7.0*3600 + 600);
		dep.setVehicleId(Id.create("19", Vehicle.class));
		route1.addDeparture(dep);

		TransitLine line1 = builder.createTransitLine(Id.create("8", TransitLine.class));
		line1.addRoute(route1);

		schedule1.addTransitLine(line1);

		// write and read it
		String filename = utils.getOutputDirectory() + "scheduleNoRoute.xml";
		new TransitScheduleWriterV1(schedule1).write(filename);
		TransitScheduleFactory builder2 = new TransitScheduleFactoryImpl();
		TransitSchedule schedule2 = builder2.createTransitSchedule();
		new TransitScheduleReaderV1(schedule2, new RouteFactories()).readFile(filename);

		// first test, without network-route
		assertEqualSchedules(schedule1, schedule2);

		// now add route info to the schedule
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(l1.getId(), l4.getId());
		List<Id<Link>> links = new ArrayList<Id<Link>>(2);
		links.add(l2.getId());
		links.add(l3.getId());
		route.setLinkIds(l1.getId(), links, l4.getId());
		TransitRoute route2 = builder.createTransitRoute(Id.create(2, TransitRoute.class), route, stops, "bus");
		line1.addRoute(route2);
		stop1.setLinkId(l1.getId());

		// write and read version with network-route
		filename = utils.getOutputDirectory() + "scheduleWithRoute.xml";
		new TransitScheduleWriterV1(schedule1).write(filename);
		TransitScheduleFactory builder3 = new TransitScheduleFactoryImpl();
		TransitSchedule schedule3 = builder3.createTransitSchedule();
		new TransitScheduleReaderV1(schedule3, new RouteFactories()).readFile(filename);

		assertEqualSchedules(schedule1, schedule3);
	}

	private static void assertEqualSchedules(final TransitSchedule expected, final TransitSchedule actual) {

		assertEquals(expected.getFacilities().size(), actual.getFacilities().size(), "different number of stopFacilities.");
		for (TransitStopFacility stopE : expected.getFacilities().values()) {
			TransitStopFacility stopA = actual.getFacilities().get(stopE.getId());
			assertNotNull(stopA, "stopFacility not found: " + stopE.getId().toString());
			assertEquals(stopE.getCoord().getX(), stopA.getCoord().getX(), MatsimTestUtils.EPSILON, "different x coordinates.");
			assertEquals(stopE.getCoord().getY(), stopA.getCoord().getY(), MatsimTestUtils.EPSILON, "different y coordinates.");
			assertEquals(stopE.getLinkId(), stopA.getLinkId(), "different link information.");
			assertEquals(stopE.getIsBlockingLane(), stopA.getIsBlockingLane(), "different isBlocking.");
			assertEquals(stopE.getName(), stopA.getName(), "different names.");
		}

		assertEquals(expected.getTransitLines().size(), actual.getTransitLines().size(), "different number of transitLines.");
		for (TransitLine lineE : expected.getTransitLines().values()) {
			// *E = expected, *A = actual
			TransitLine lineA = actual.getTransitLines().get(lineE.getId());
			assertNotNull(lineA, "transit line not found: " + lineE.getId().toString());
			assertEquals(lineE.getRoutes().size(), lineA.getRoutes().size(), "different number of routes in line.");
			for (TransitRoute routeE : lineE.getRoutes().values()) {
				TransitRoute routeA = lineA.getRoutes().get(routeE.getId());
				assertNotNull(routeA, "transit route not found: " + routeE.getId().toString());
				assertEquals(routeE.getDescription(), routeA.getDescription(), "different route descriptions.");

				assertEquals(routeE.getStops().size(), routeA.getStops().size(), "different number of stops.");
				for (int i = 0, n = routeE.getStops().size(); i < n; i++) {
					TransitRouteStop stopE = routeE.getStops().get(i);
					TransitRouteStop stopA = routeA.getStops().get(i);
					assertNotNull(stopA, "stop not found");
					assertEquals(stopE.getStopFacility().getId(), stopA.getStopFacility().getId(), "different stop facilities.");
					assertEquals(stopE.getArrivalOffset(), stopA.getArrivalOffset(), "different arrival delay.");
					assertEquals(stopE.getDepartureOffset(), stopA.getDepartureOffset(), "different departure delay.");
					assertEquals(stopE.isAwaitDepartureTime(), stopA.isAwaitDepartureTime(), "different awaitDepartureTime.");
				}

				NetworkRoute netRouteE = routeE.getRoute();
				if (netRouteE == null) {
					assertNull(routeA.getRoute(), "bad network route, must be null.");
				} else {
					NetworkRoute netRouteA = routeA.getRoute();
					assertNotNull(netRouteA, "bad network route, must not be null.");
					assertEquals(netRouteE.getStartLinkId(), netRouteA.getStartLinkId(), "wrong start link.");
					assertEquals(netRouteE.getEndLinkId(), netRouteA.getEndLinkId(), "wrong end link.");
					List<Id<Link>> linkIdsE = netRouteE.getLinkIds();
					List<Id<Link>> linkIdsA = netRouteA.getLinkIds();
					for (int i = 0, n = linkIdsE.size(); i < n; i++) {
						assertEquals(linkIdsE.get(i), linkIdsA.get(i), "wrong link in network route");
					}
				}

				assertEquals(routeE.getDepartures().size(), routeA.getDepartures().size(), "different number of departures in route.");
				for (Departure departureE : routeE.getDepartures().values()) {
					Departure departureA = routeA.getDepartures().get(departureE.getId());
					assertNotNull(departureA, "departure not found: " + departureE.getId().toString());
					assertEquals(departureE.getDepartureTime(), departureA.getDepartureTime(), MatsimTestUtils.EPSILON, "different departure times.");
					assertEquals(departureE.getVehicleId(), departureA.getVehicleId(), "different vehicle ids.");
				}
			}
		}

	}
}
