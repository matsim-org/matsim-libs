/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterTest.java
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

package playground.marcel.pt.router;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;

import playground.marcel.pt.transitSchedule.TransitScheduleImpl;
import playground.marcel.pt.transitSchedule.TransitScheduleReaderTest;
import playground.marcel.pt.transitSchedule.TransitScheduleReaderV1;

public class TransitRouterTest extends MatsimTestCase {

	private static final String INPUT_TEST_FILE_TRANSITSCHEDULE = "transitSchedule.xml";
	private static final String INPUT_TEST_FILE_NETWORK = "network.xml";

	public void testGetNextDepartures() throws SAXException, ParserConfigurationException, IOException {

		String inputDir = "test/input/" + TransitScheduleReaderTest.class.getPackage().getName().replace('.', '/') + "/";
		inputDir = "../thesis-data/examples/minibln/";

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(inputDir + INPUT_TEST_FILE_NETWORK);

		TransitScheduleImpl schedule = new TransitScheduleImpl();
		new TransitScheduleReaderV1(schedule, network).readFile(inputDir + INPUT_TEST_FILE_TRANSITSCHEDULE);

		TransitRouter router = new TransitRouter(schedule);
//		router.getNextDeparturesAtStop(schedule.getFacilities().get(new IdImpl("stop2")), Time.parseTime("07:01:00"));
		router.getNextDeparturesAtStop(schedule.getFacilities().get(new IdImpl("1")), Time.parseTime("07:01:00"));

		// TODO [MR] missing assert-statement, until now the code just tests if it compiles/runs without Exception
	}

	public void testCalcRoute() throws SAXException, ParserConfigurationException, IOException {
		String inputDir = "test/input/" + TransitScheduleReaderTest.class.getPackage().getName().replace('.', '/') + "/";
		inputDir = "../thesis-data/examples/minibln/";

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(inputDir + INPUT_TEST_FILE_NETWORK);

		TransitScheduleImpl schedule = new TransitScheduleImpl();
		new TransitScheduleReaderV1(schedule, network).readFile(inputDir + INPUT_TEST_FILE_TRANSITSCHEDULE);

		TransitRouter router = new TransitRouter(schedule);
		//		router.calcRoute(new CoordImpl(200, 200), new CoordImpl(4800, 800), Time.parseTime("07:01:00"));
		List<Id> linkIds = router.calcRoute(schedule.getFacilities().get(new IdImpl("h")).getCoord(),
				schedule.getFacilities().get(new IdImpl("l2")).getCoord(),
				Time.parseTime("07:01:00"));
		assertNotNull(linkIds);
		assertEquals(7, linkIds.size());
		assertEquals(new IdImpl(0), linkIds.get(0));
		assertEquals(new IdImpl(1), linkIds.get(1));
		assertEquals(new IdImpl(12), linkIds.get(2));
		assertEquals(new IdImpl(9), linkIds.get(3));
		assertEquals(new IdImpl(10), linkIds.get(4));
		assertEquals(new IdImpl(16), linkIds.get(5));
		assertEquals(new IdImpl(7), linkIds.get(6));
	}
}
