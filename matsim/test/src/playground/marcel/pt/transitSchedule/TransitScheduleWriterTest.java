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

package playground.marcel.pt.transitSchedule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.FacilitiesImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.LinkNetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;

public class TransitScheduleWriterTest extends MatsimTestCase {

	public void testWrite() throws IOException, SAXException, ParserConfigurationException {
		// prepare required data
		NetworkLayer network = new NetworkLayer();
		Node n1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node n2 = network.createNode(new IdImpl("2"), new CoordImpl(0, 0));
		Node n3 = network.createNode(new IdImpl("3"), new CoordImpl(0, 0));
		Node n4 = network.createNode(new IdImpl("4"), new CoordImpl(0, 0));
		Node n5 = network.createNode(new IdImpl("5"), new CoordImpl(0, 0));
		Link l1 = network.createLink(new IdImpl("1"), n1, n2, 1000, 10, 3600, 1.0);
		Link l2 = network.createLink(new IdImpl("2"), n2, n3, 1000, 10, 3600, 1.0);
		Link l3 = network.createLink(new IdImpl("3"), n3, n4, 1000, 10, 3600, 1.0);
		Link l4 = network.createLink(new IdImpl("4"), n4, n5, 1000, 10, 3600, 1.0);
		Facilities facilities = new FacilitiesImpl();

		Facility stop1 = facilities.createFacility(new IdImpl("stop1"), new CoordImpl(0, 0));
		Facility stop2 = facilities.createFacility(new IdImpl("stop2"), new CoordImpl(1000, 0));
		Facility stop3 = facilities.createFacility(new IdImpl("stop3"), new CoordImpl(1000, 1000));
		Facility stop4 = facilities.createFacility(new IdImpl("stop4"), new CoordImpl(0, 1000));

		// prepare some schedule, without network-route
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>(4);
		stops.add(new TransitRouteStop(stop1, Time.UNDEFINED_TIME, 0));
		stops.add(new TransitRouteStop(stop2, 90, 120));
		stops.add(new TransitRouteStop(stop3, Time.UNDEFINED_TIME, 300));
		stops.add(new TransitRouteStop(stop4, 400, Time.UNDEFINED_TIME));

		TransitRoute route1 = new TransitRoute(new IdImpl(1), null, stops);
		route1.setDescription("Just a comment.");

		route1.addDeparture(new Departure(new IdImpl("2"), 7.0*3600));
		route1.addDeparture(new Departure(new IdImpl("4"), 7.0*3600 + 300));
		route1.addDeparture(new Departure(new IdImpl("7"), 7.0*3600 + 600));

		TransitLine line1 = new TransitLine(new IdImpl("8"));
		line1.addRoute(route1);

		TransitSchedule schedule1 = new TransitSchedule();
		schedule1.addTransitLine(line1);

		// write and read it
		String filename = getOutputDirectory() + "scheduleNoRoute.xml";
		new TransitScheduleWriter(schedule1).write(filename);
		TransitSchedule schedule2 = new TransitSchedule();
		new TransitScheduleReader(schedule2, network, facilities).readFile(filename);

		// first test, without network-route
		assertEquals(schedule1, schedule2);

		// now add route info to the schedule
		NetworkRoute route = new LinkNetworkRoute(l1, l4);
		List<Link> links = new ArrayList<Link>(2);
		links.add(l2);
		links.add(l3);
		route.setLinks(l1, links, l4);
		TransitRoute route2 = new TransitRoute(new IdImpl(2), route, stops);
		line1.addRoute(route2);

		// write and read version with network-route
		filename = getOutputDirectory() + "scheduleWithRoute.xml";
		new TransitScheduleWriter(schedule1).write(filename);
		TransitSchedule schedule3 = new TransitSchedule();
		new TransitScheduleReader(schedule3, network, facilities).readFile(filename);

		assertEquals(schedule1, schedule3);
	}

	private static void assertEquals(final TransitSchedule expected, final TransitSchedule actual) {
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
					assertEquals("different stop facilities.", stopE.getStopFacility(), stopA.getStopFacility());
					assertEquals("different arrival delay.", stopE.getArrivalDelay(), stopA.getArrivalDelay(), EPSILON);
					assertEquals("different departure delay.", stopE.getDepartureDelay(), stopA.getDepartureDelay(), EPSILON);
				}

				NetworkRoute netRouteE = routeE.getRoute();
				if (netRouteE == null) {
					assertNull("bad network route, must be null.", routeA.getRoute());
				} else {
					NetworkRoute netRouteA = routeA.getRoute();
					assertNotNull("bad network route, must not be null.", netRouteA);
					assertEquals("wrong start link.", netRouteE.getStartLink(), netRouteA.getStartLink());
					assertEquals("wrong end link.", netRouteE.getEndLink(), netRouteA.getEndLink());
					List<Link> linksE = netRouteE.getLinks();
					List<Link> linksA = netRouteA.getLinks();
					for (int i = 0, n = linksE.size(); i < n; i++) {
						assertEquals("wrong link in network route", linksE.get(i), linksA.get(i));
					}
				}

				assertEquals("different number of departures in route.", routeE.getDepartures().size(), routeA.getDepartures().size());
				for (Departure departureE : routeE.getDepartures().values()) {
					Departure departureA = routeA.getDepartures().get(departureE.getId());
					assertNotNull("departure not found: " + departureE.getId().toString(), departureA);
					assertEquals("different departure times.", departureE.getDepartureTime(), departureA.getDepartureTime(), EPSILON);
				}
			}
		}

	}
}
