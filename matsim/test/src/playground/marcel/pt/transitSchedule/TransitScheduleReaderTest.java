/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleReaderTest.java
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
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.FacilitiesImpl;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Route;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.world.World;
import org.xml.sax.SAXException;

public class TransitScheduleReaderTest extends MatsimTestCase {

	public static final String INPUT_TEST_FILE_TRANSITSCHEDULE = "transitSchedule.xml";
	public static final String INPUT_TEST_FILE_NETWORK = "network.xml";
	public static final String INPUT_TEST_FILE_FACILITIES = "facilities.xml";

	public void testReadFile_General() throws SAXException, ParserConfigurationException, IOException {
		final String inputDir = getPackageInputDirectory();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(inputDir + INPUT_TEST_FILE_NETWORK);
		FacilitiesImpl facilities = new FacilitiesImpl();
		new MatsimFacilitiesReader(facilities).readFile(inputDir + INPUT_TEST_FILE_FACILITIES);

		World world = new World();
		world.setFacilityLayer(facilities);
		world.setNetworkLayer(network);
		world.complete();

		TransitSchedule schedule = new TransitSchedule();
		new TransitScheduleReader(schedule, network, facilities).readFile(inputDir + INPUT_TEST_FILE_TRANSITSCHEDULE);

		assertEquals("wrong number of transit lines.", 1, schedule.getTransitLines().size());
		assertEquals("wrong line id.", new IdImpl("T1"), schedule.getTransitLines().keySet().iterator().next());

		TransitLine lineT1 = schedule.getTransitLine(new IdImpl("T1"));
		assertNotNull("could not find line with id T1.", lineT1);

		TransitRoute route1 = lineT1.getRoute(new IdImpl("1"));
		assertNotNull("could not find route 1 in line T1.", route1);

		Map<Id, Departure> departures = route1.getDepartures();
		assertNotNull("could not get departures of route 1 in line T1.", departures);
		assertEquals("wrong number of departures.", 3, departures.size());

		List<TransitRouteStop> stops = route1.getStops();
		assertNotNull("could not get transit route stops.", stops);
		assertEquals("wrong number of stops.", 6, stops.size());

		Route route = route1.getRoute();
		assertNotNull("could not get route.", route);
		assertEquals("wrong number of links in route.", 4, route.getLinkIds().size());
	}

}
