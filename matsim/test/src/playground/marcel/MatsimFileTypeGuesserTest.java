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

package playground.marcel;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class MatsimFileTypeGuesserTest extends MatsimTestCase {

	private final static Logger log = Logger.getLogger(MatsimFileTypeGuesserTest.class);

	public void testNetworkV1Dtd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/scenarios/equil/network.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Network, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertEquals("http://www.matsim.org/files/dtd/network_v1.dtd", g.getSystemId());
	}

	public void testConfigV1Dtd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/scenarios/equil/config.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Config, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertEquals("http://www.matsim.org/files/dtd/config_v1.dtd", g.getSystemId());
	}

	public void testPlansV4Dtd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/scenarios/equil/plans100.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Population, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertEquals("http://www.matsim.org/files/dtd/plans_v4.dtd", g.getSystemId());
	}

	public void testPlansV5Xsd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/basic/v01/testPopulation.xml");
		assertNull(g.getPublicId());
		assertEquals(MatsimFileTypeGuesser.FileType.Population, g.getGuessedFileType());
		assertEquals("http://www.matsim.org/files/dtd/population_v5.0.xsd", g.getSystemId());
		fail("needs better example file");
	}

	public void testCountsV1Xsd() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("examples/equil/counts100.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Counts, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertEquals("http://matsim.org/files/dtd/counts_v1.xsd", g.getSystemId());
		fail("needs better example file");
	}

	public void testEventsV1Txt() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/events/EventsReadersTest/events.txt");
		assertEquals(MatsimFileTypeGuesser.FileType.Events, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertNull(g.getSystemId());
		fail("needs better example file");
	}

	public void testEventsV1Xml() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/events/EventsReadersTest/events.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.Events, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertNull(g.getSystemId());
		fail("needs better example file");
	}

	// TODO needs more test for Households, Transims-Veh, OTFVis, ...

	public void testNotExistant() {
		try {
			new MatsimFileTypeGuesser("examples/equil/dummy.xml");
			fail("expected IOException");
		} catch (IOException e) {
			log.info("catched expected exception: " + e.getMessage());
		}
	}

}
