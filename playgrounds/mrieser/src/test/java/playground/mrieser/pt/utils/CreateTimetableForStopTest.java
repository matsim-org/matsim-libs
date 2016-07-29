/* *********************************************************************** *
 * project: org.matsim.*
 * CreateTimetableForStopTest.java
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

package playground.mrieser.pt.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderTest;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;


public class CreateTimetableForStopTest {

	public static final String INPUT_TEST_FILE_TRANSITSCHEDULE = "transitSchedule.xml";
	public static final String INPUT_TEST_FILE_NETWORK = "network.xml";

	@Test
	public void testGetDeparturesAtStop() throws SAXException, ParserConfigurationException, IOException {
		final String inputDir = "test/input/" + TransitScheduleReaderTest.class.getCanonicalName().replace('.', '/') + "/";

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(inputDir + INPUT_TEST_FILE_NETWORK);

		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = builder.createTransitSchedule();
		new TransitScheduleReaderV1(schedule, network).readFile(inputDir + INPUT_TEST_FILE_TRANSITSCHEDULE);

		TransitLine line = schedule.getTransitLines().get(Id.create("T1", TransitLine.class));
		CreateTimetableForStop timetable = new CreateTimetableForStop(line);
		assertNotNull("could not get transit line.", line);

		double[] departures = timetable.getDeparturesAtStop(schedule.getFacilities().get(Id.create("stop3", TransitStopFacility.class)));

		for (double d : departures) {
			System.out.println("Departure at " + Time.writeTime(d));
		}

		assertEquals("wrong number of departures.", 3, departures.length);
		double baseDepartureTime = Time.parseTime("07:00:00") + Time.parseTime("00:03:00");
		assertEquals("wrong departure time for 1st departure.", baseDepartureTime, departures[0], MatsimTestCase.EPSILON);
		assertEquals("wrong departure time for 2nd departure.", baseDepartureTime + 600, departures[1], MatsimTestCase.EPSILON);
		assertEquals("wrong departure time for 3rd departure.", baseDepartureTime + 1200, departures[2], MatsimTestCase.EPSILON);
	}
}
