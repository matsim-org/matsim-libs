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

package org.matsim.transitSchedule;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.matsim.transitSchedule.api.TransitScheduleReader;
import org.xml.sax.SAXException;


/**
 * @author mrieser
 */
public class TransitScheduleReaderTest extends MatsimTestCase {

	private static final String INPUT_TEST_FILE_TRANSITSCHEDULE = "transitSchedule.xml";
	private static final String INPUT_TEST_FILE_NETWORK = "network.xml";

	public void testReadFileV1() throws SAXException, ParserConfigurationException, IOException {
		final String inputDir = getClassInputDirectory();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(inputDir + INPUT_TEST_FILE_NETWORK);

		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = builder.createTransitSchedule();
		new TransitScheduleReaderV1(schedule, network).readFile(inputDir + INPUT_TEST_FILE_TRANSITSCHEDULE);

		assertEquals("wrong number of transit lines.", 1, schedule.getTransitLines().size());
		assertEquals("wrong line id.", new IdImpl("T1"), schedule.getTransitLines().keySet().iterator().next());

		TransitLine lineT1 = schedule.getTransitLines().get(new IdImpl("T1"));
		assertNotNull("could not find line with id T1.", lineT1);

		TransitRoute route1 = lineT1.getRoutes().get(new IdImpl("1"));
		assertNotNull("could not find route 1 in line T1.", route1);

		Map<Id, Departure> departures = route1.getDepartures();
		assertNotNull("could not get departures of route 1 in line T1.", departures);
		assertEquals("wrong number of departures.", 3, departures.size());

		List<TransitRouteStop> stops = route1.getStops();
		assertNotNull("could not get transit route stops.", stops);
		assertEquals("wrong number of stops.", 6, stops.size());

		NetworkRouteWRefs route = route1.getRoute();
		assertNotNull("could not get route.", route);
		assertEquals("wrong start link.", network.getLinks().get(new IdImpl("1")), route.getStartLink());
		assertEquals("wrong end link.", network.getLinks().get(new IdImpl("8")), route.getEndLink());
		assertEquals("wrong number of links in route.", 4, route.getLinkIds().size());
	}

	public void testReadFile() throws IOException, SAXException, ParserConfigurationException {
		final String inputDir = getClassInputDirectory();

		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().scenario().setUseTransit(true);

		new MatsimNetworkReader(scenario.getNetwork()).readFile(inputDir + INPUT_TEST_FILE_NETWORK);

		new TransitScheduleReader(scenario).readFile(inputDir + INPUT_TEST_FILE_TRANSITSCHEDULE);

		// only a minimal test, the actual reader used should be tested somewhere separately
		assertEquals("wrong number of transit lines.", 1, scenario.getTransitSchedule().getTransitLines().size());
		// in the end, we mostly test that there is no Exception when reading the file.

	}

}
