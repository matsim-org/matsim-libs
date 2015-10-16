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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestCase;
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
public class TransitScheduleFormatV1Test extends MatsimTestCase {

	public void testWriteRead() throws IOException, SAXException, ParserConfigurationException {
		// prepare required data
		NetworkImpl network = NetworkImpl.createNetwork();
		Node n1 = network.createAndAddNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node n2 = network.createAndAddNode(Id.create("2", Node.class), new Coord((double) 0, (double) 0));
		Node n3 = network.createAndAddNode(Id.create("3", Node.class), new Coord((double) 0, (double) 0));
		Node n4 = network.createAndAddNode(Id.create("4", Node.class), new Coord((double) 0, (double) 0));
		Node n5 = network.createAndAddNode(Id.create("5", Node.class), new Coord((double) 0, (double) 0));
		Link l1 = network.createAndAddLink(Id.create("1", Link.class), n1, n2, 1000, 10, 3600, 1.0);
		Link l2 = network.createAndAddLink(Id.create("2", Link.class), n2, n3, 1000, 10, 3600, 1.0);
		Link l3 = network.createAndAddLink(Id.create("3", Link.class), n3, n4, 1000, 10, 3600, 1.0);
		Link l4 = network.createAndAddLink(Id.create("4", Link.class), n4, n5, 1000, 10, 3600, 1.0);

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
		stops.add(builder.createTransitRouteStop(stop1, Time.UNDEFINED_TIME, 0));
		stops.add(builder.createTransitRouteStop(stop2, 90, 120));
		TransitRouteStop routeStop3 = builder.createTransitRouteStop(stop3, Time.UNDEFINED_TIME, 300);
		routeStop3.setAwaitDepartureTime(true);
		stops.add(routeStop3);
		stops.add(builder.createTransitRouteStop(stop4, 400, Time.UNDEFINED_TIME));

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
		String filename = getOutputDirectory() + "scheduleNoRoute.xml";
		new TransitScheduleWriterV1(schedule1).write(filename);
		TransitScheduleFactory builder2 = new TransitScheduleFactoryImpl();
		TransitSchedule schedule2 = builder2.createTransitSchedule();
		new TransitScheduleReaderV1(schedule2, network).readFile(filename);

		// first test, without network-route
		assertEquals(schedule1, schedule2);

		// now add route info to the schedule
		NetworkRoute route = new LinkNetworkRouteImpl(l1.getId(), l4.getId());
		List<Id<Link>> links = new ArrayList<Id<Link>>(2);
		links.add(l2.getId());
		links.add(l3.getId());
		route.setLinkIds(l1.getId(), links, l4.getId());
		TransitRoute route2 = builder.createTransitRoute(Id.create(2, TransitRoute.class), route, stops, "bus");
		line1.addRoute(route2);
		stop1.setLinkId(l1.getId());

		// write and read version with network-route
		filename = getOutputDirectory() + "scheduleWithRoute.xml";
		new TransitScheduleWriterV1(schedule1).write(filename);
		TransitScheduleFactory builder3 = new TransitScheduleFactoryImpl();
		TransitSchedule schedule3 = builder3.createTransitSchedule();
		new TransitScheduleReaderV1(schedule3, network).readFile(filename);

		assertEquals(schedule1, schedule3);
	}

	private static void assertEquals(final TransitSchedule expected, final TransitSchedule actual) {

		assertEquals("different number of stopFacilities.", expected.getFacilities().size(), actual.getFacilities().size());
		for (TransitStopFacility stopE : expected.getFacilities().values()) {
			TransitStopFacility stopA = actual.getFacilities().get(stopE.getId());
			assertNotNull("stopFacility not found: " + stopE.getId().toString(), stopA);
			assertEquals("different x coordinates.", stopE.getCoord().getX(), stopA.getCoord().getX(), EPSILON);
			assertEquals("different y coordinates.", stopE.getCoord().getY(), stopA.getCoord().getY(), EPSILON);
			assertEquals("different link information.", stopE.getLinkId(), stopA.getLinkId());
			assertEquals("different isBlocking.", stopE.getIsBlockingLane(), stopA.getIsBlockingLane());
			assertEquals("different names.", stopE.getName(), stopA.getName());
		}

		assertEquals("different number of transitLines.", expected.getTransitLines().size(), actual.getTransitLines().size());
		for (TransitLine lineE : expected.getTransitLines().values()) {
			// *E = expected, *A = actual
			TransitLine lineA = actual.getTransitLines().get(lineE.getId());
			assertNotNull("transit line not found: " + lineE.getId().toString(), lineA);
			assertEquals("different number of routes in line.", lineE.getRoutes().size(), lineA.getRoutes().size());
			for (TransitRoute routeE : lineE.getRoutes().values()) {
				TransitRoute routeA = lineA.getRoutes().get(routeE.getId());
				assertNotNull("transit route not found: " + routeE.getId().toString(), routeA);
				assertEquals("different route descriptions.", routeE.getDescription(), routeA.getDescription());

				assertEquals("different number of stops.", routeE.getStops().size(), routeA.getStops().size());
				for (int i = 0, n = routeE.getStops().size(); i < n; i++) {
					TransitRouteStop stopE = routeE.getStops().get(i);
					TransitRouteStop stopA = routeA.getStops().get(i);
					assertNotNull("stop not found", stopA);
					assertEquals("different stop facilities.", stopE.getStopFacility().getId(), stopA.getStopFacility().getId());
					assertEquals("different arrival delay.", stopE.getArrivalOffset(), stopA.getArrivalOffset(), EPSILON);
					assertEquals("different departure delay.", stopE.getDepartureOffset(), stopA.getDepartureOffset(), EPSILON);
					assertEquals("different awaitDepartureTime.", stopE.isAwaitDepartureTime(), stopA.isAwaitDepartureTime());
				}

				NetworkRoute netRouteE = routeE.getRoute();
				if (netRouteE == null) {
					assertNull("bad network route, must be null.", routeA.getRoute());
				} else {
					NetworkRoute netRouteA = routeA.getRoute();
					assertNotNull("bad network route, must not be null.", netRouteA);
					assertEquals("wrong start link.", netRouteE.getStartLinkId(), netRouteA.getStartLinkId());
					assertEquals("wrong end link.", netRouteE.getEndLinkId(), netRouteA.getEndLinkId());
					List<Id<Link>> linkIdsE = netRouteE.getLinkIds();
					List<Id<Link>> linkIdsA = netRouteA.getLinkIds();
					for (int i = 0, n = linkIdsE.size(); i < n; i++) {
						assertEquals("wrong link in network route", linkIdsE.get(i), linkIdsA.get(i));
					}
				}

				assertEquals("different number of departures in route.", routeE.getDepartures().size(), routeA.getDepartures().size());
				for (Departure departureE : routeE.getDepartures().values()) {
					Departure departureA = routeA.getDepartures().get(departureE.getId());
					assertNotNull("departure not found: " + departureE.getId().toString(), departureA);
					assertEquals("different departure times.", departureE.getDepartureTime(), departureA.getDepartureTime(), EPSILON);
					assertEquals("different vehicle ids.", departureE.getVehicleId(), departureA.getVehicleId());
				}
			}
		}

	}
}
