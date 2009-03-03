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

package playground.marcel.pt.transitSchedule.modules;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.FacilitiesImpl;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.misc.Time;
import org.matsim.world.World;
import org.xml.sax.SAXException;

import playground.marcel.pt.transitSchedule.TransitLine;
import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.marcel.pt.transitSchedule.TransitScheduleReader;
import playground.marcel.pt.transitSchedule.TransitScheduleReaderTest;

public class CreateTimetableForStopTest extends MatsimTestCase {

	public static final String INPUT_TEST_FILE_TRANSITSCHEDULE = "transitSchedule.xml";
	public static final String INPUT_TEST_FILE_NETWORK = "network.xml";
	public static final String INPUT_TEST_FILE_FACILITIES = "facilities.xml";

	public void testGetDeparturesAtStop() throws SAXException, ParserConfigurationException, IOException {
		final String inputDir = "test/input/" + TransitScheduleReaderTest.class.getPackage().getName().replace('.', '/') + "/";

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

		TransitLine line = schedule.getTransitLine(new IdImpl("T1"));
		CreateTimetableForStop timetable = new CreateTimetableForStop(line);
		assertNotNull("could not get transit line.", line);

		double[] departures = timetable.getDeparturesAtStop(facilities.getFacility(new IdImpl("stop3")));

		for (double d : departures) {
			System.out.println("Departure at " + Time.writeTime(d));
		}

		assertEquals("wrong number of departures.", 3, departures.length);
		double baseDepartureTime = Time.parseTime("07:00:00") + Time.parseTime("00:03:00");
		assertEquals("wrong departure time for 1st departure.", baseDepartureTime, departures[0], EPSILON);
		assertEquals("wrong departure time for 2nd departure.", baseDepartureTime + 600, departures[1], EPSILON);
		assertEquals("wrong departure time for 3rd departure.", baseDepartureTime + 1200, departures[2], EPSILON);
	}
}
