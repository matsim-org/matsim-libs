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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.lanes.data.v20.LaneDefinitionsReader;

/**
 * @author mrieser
 */
public class MatsimFileTypeGuesserTest {


	private final static Logger log = Logger.getLogger(MatsimFileTypeGuesserTest.class);

	@Test
	public void testNetworkV1Dtd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/scenarios/equil/network.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Network, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertEquals("http://www.matsim.org/files/dtd/network_v1.dtd", g.getSystemId());
	}

	@Test
	public void testConfigV1Dtd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/scenarios/equil/config.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Config, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertEquals("http://www.matsim.org/files/dtd/config_v1.dtd", g.getSystemId());
	}

	@Test
	public void testPlansV4Dtd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/scenarios/equil/plans100.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Population, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertEquals("http://www.matsim.org/files/dtd/plans_v4.dtd", g.getSystemId());
	}

	@Test
	public void testPopulationV5Dtd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/core/utils/io/MatsimFileTypeGuesserTest/population_v5_example.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Population, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertEquals("http://www.matsim.org/files/dtd/population_v5.dtd", g.getSystemId());
	}

	@Test
	public void testFacilitiesV1Dtd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/scenarios/equil/facilities.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Facilities, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertEquals("http://www.matsim.org/files/dtd/facilities_v1.dtd", g.getSystemId());
	}

	@Test
	public void testCountsV1Xsd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("examples/equil/counts100.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Counts, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertEquals("http://matsim.org/files/dtd/counts_v1.xsd", g.getSystemId());
	}

	@Test
	public void testEventsV1Txt() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/events/EventsReadersTest/events.txt");
		assertEquals(MatsimFileTypeGuesser.FileType.Events, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertNull(g.getSystemId());
	}

	@Test
	public void testEventsV1Xml() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/core/events/EventsReadersTest/events.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Events, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertNull(g.getSystemId());
	}

	@Test
	public void testLaneDefinitionsV11XML() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/lanes/data/v20/LaneDefinitionsReaderWriterTest/testLaneDefinitions_v1.1.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.LaneDefinitions, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertNotNull(g.getSystemId());
		assertEquals(LaneDefinitionsReader.SCHEMALOCATIONV11, g.getSystemId());
	}

	@Test
	public void testTransitScheduleV1XML() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/pt/transitSchedule/TransitScheduleReaderTest/transitSchedule.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.TransitSchedule, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertNotNull(g.getSystemId());
		assertEquals("http://www.matsim.org/files/dtd/transitSchedule_v1.dtd", g.getSystemId());
	}

	@Test
	public void testVehiclesV1XML() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/vehicles/testVehicles.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Vehicles, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertNotNull(g.getSystemId());
		assertEquals("http://www.matsim.org/files/dtd/vehicleDefinitions_v1.0.xsd", g.getSystemId());
	}

	@Test
	public void testObjectAttributesV1XML_withDtd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/utils/objectattributes/objectattributes_withDtd_v1.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.ObjectAttributes, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertNotNull(g.getSystemId());
		assertEquals("http://matsim.org/files/dtd/objectattributes_v1.dtd", g.getSystemId());
	}

	@Test
	public void testObjectAttributesV1XML_withoutDtd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/utils/objectattributes/objectattributes_withoutDtd_v1.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.ObjectAttributes, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertNull(g.getSystemId());
	}

	@Test
	public void testNotExistant() {
		try {
			new MatsimFileTypeGuesser("examples/equil/dummy.xml");
			fail("expected IOException");
		} catch (UncheckedIOException e) {
			log.info("catched expected exception: " + e.getMessage());
		}
	}

}
