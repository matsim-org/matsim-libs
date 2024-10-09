/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimFileTypeGuesserTest.java
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

package org.matsim.core.utils.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.matsim.lanes.LanesReader;

/**
 * @author mrieser
 */
public class MatsimFileTypeGuesserTest {


	private final static Logger log = LogManager.getLogger(MatsimFileTypeGuesserTest.class);

	@Test
	void testNetworkV1Dtd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/scenarios/equil/network.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Network, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertEquals("http://www.matsim.org/files/dtd/network_v1.dtd", g.getSystemId());
	}

	@Test
	void testConfigV2Dtd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/scenarios/equil/config.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Config, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertEquals("http://www.matsim.org/files/dtd/config_v2.dtd", g.getSystemId());
	}

	@Test
	void testPlansV4Dtd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/scenarios/equil/plans100.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Population, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertEquals("http://www.matsim.org/files/dtd/plans_v4.dtd", g.getSystemId());
	}

	@Test
	void testPopulationV5Dtd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/core/utils/io/MatsimFileTypeGuesserTest/population_v5_example.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Population, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertEquals("http://www.matsim.org/files/dtd/population_v5.dtd", g.getSystemId());
	}

	@Test
	void testFacilitiesV1Dtd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/scenarios/equil/facilities.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Facilities, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertEquals("http://www.matsim.org/files/dtd/facilities_v1.dtd", g.getSystemId());
	}

	@Test
	void testCountsV1Xsd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/scenarios/equil/counts100.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Counts, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertEquals("http://matsim.org/files/dtd/counts_v1.xsd", g.getSystemId());
	}

	@Test
	void testEventsV1Txt() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/events/EventsReadersTest/events.txt");
		assertEquals(MatsimFileTypeGuesser.FileType.Events, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertNull(g.getSystemId());
	}

	@Test
	void testEventsV1Xml() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/core/events/EventsReadersTest/events.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Events, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertNull(g.getSystemId());
	}

	@Test
	void testLanesV20XML() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/lanes/data/LanesReaderWriterTest/testLanes.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.LaneDefinitions, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertNotNull(g.getSystemId());
		assertEquals(LanesReader.SCHEMALOCATIONV20, g.getSystemId());
	}

	@Test
	void testTransitScheduleV1XML() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/pt/transitSchedule/TransitScheduleReaderTest/transitSchedule.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.TransitSchedule, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertNotNull(g.getSystemId());
		assertEquals("http://www.matsim.org/files/dtd/transitSchedule_v1.dtd", g.getSystemId());
	}

	@Test
	void testVehiclesV1XML() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/vehicles/testVehicles_v1.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Vehicles, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertNotNull(g.getSystemId());
		assertEquals("http://www.matsim.org/files/dtd/vehicleDefinitions_v1.0.xsd", g.getSystemId());
	}

	@Test
	void testObjectAttributesV1XML_withDtd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/utils/objectattributes/objectattributes_withDtd_v1.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.ObjectAttributes, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertNotNull(g.getSystemId());
		assertEquals("http://matsim.org/files/dtd/objectattributes_v1.dtd", g.getSystemId());
	}

	@Test
	void testObjectAttributesV1XML_withoutDtd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/utils/objectattributes/objectattributes_withoutDtd_v1.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.ObjectAttributes, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertNull(g.getSystemId());
	}

	@Test
	void testNotExistant() {
		try {
			new MatsimFileTypeGuesser("examples/equil/dummy.xml");
			fail("expected IOException");
		} catch (UncheckedIOException e) {
			log.info("catched expected exception: " + e.getMessage());
		}
	}

}
