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

package org.matsim.pt.transitSchedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.testcases.MatsimTestUtils;
import org.xml.sax.SAXException;


/**
 * @author mrieser
 */
public class TransitScheduleReaderTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	private static final String INPUT_TEST_FILE_TRANSITSCHEDULE = "transitSchedule.xml";
	private static final String INPUT_TEST_FILE_NETWORK = "network.xml";

	@Test
	void testReadFileV1() throws SAXException, ParserConfigurationException, IOException {
		final String inputDir = utils.getClassInputDirectory();

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(inputDir + INPUT_TEST_FILE_NETWORK);

		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = builder.createTransitSchedule();
		new TransitScheduleReaderV1(schedule, scenario.getPopulation().getFactory().getRouteFactories()).readFile(inputDir + INPUT_TEST_FILE_TRANSITSCHEDULE);

		assertEquals(1, schedule.getTransitLines().size(), "wrong number of transit lines.");
		assertEquals(Id.create("T1", TransitLine.class), schedule.getTransitLines().keySet().iterator().next(), "wrong line id.");

		TransitLine lineT1 = schedule.getTransitLines().get(Id.create("T1", TransitLine.class));
		assertNotNull(lineT1, "could not find line with id T1.");

		TransitRoute route1 = lineT1.getRoutes().get(Id.create("1", TransitRoute.class));
		assertNotNull(route1, "could not find route 1 in line T1.");

		Map<Id<Departure>, Departure> departures = route1.getDepartures();
		assertNotNull(departures, "could not get departures of route 1 in line T1.");
		assertEquals(3, departures.size(), "wrong number of departures.");

		List<TransitRouteStop> stops = route1.getStops();
		assertNotNull(stops, "could not get transit route stops.");
		assertEquals(6, stops.size(), "wrong number of stops.");

		NetworkRoute route = route1.getRoute();
		assertNotNull(route, "could not get route.");
		assertEquals(network.getLinks().get(Id.create("1", Link.class)).getId(), route.getStartLinkId(), "wrong start link.");
		assertEquals(network.getLinks().get(Id.create("8", Link.class)).getId(), route.getEndLinkId(), "wrong end link.");
		assertEquals(4, route.getLinkIds().size(), "wrong number of links in route.");
	}

	@Test
	void testReadFile() throws IOException, SAXException, ParserConfigurationException {
		final String inputDir = utils.getClassInputDirectory();

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);

		new MatsimNetworkReader(scenario.getNetwork()).readFile(inputDir + INPUT_TEST_FILE_NETWORK);

		new TransitScheduleReader(scenario).readFile(inputDir + INPUT_TEST_FILE_TRANSITSCHEDULE);

		// only a minimal test, the actual reader used should be tested somewhere separately
		assertEquals(1, scenario.getTransitSchedule().getTransitLines().size(), "wrong number of transit lines.");
		// in the end, we mostly test that there is no Exception when reading the file.

	}

}
