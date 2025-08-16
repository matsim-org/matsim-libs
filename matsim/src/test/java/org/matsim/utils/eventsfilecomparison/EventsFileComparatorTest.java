/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.utils.eventsfilecomparison;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.matsim.utils.eventsfilecomparison.ComparisonResult.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author mrieser
 * @author laemmel
 */
public class EventsFileComparatorTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testRetCode0() {
		String f1 = utils.getClassInputDirectory() + "/events0.xml.gz";
		String f2 = utils.getClassInputDirectory() + "/events5.xml.gz";
		assertEquals(FILES_ARE_EQUAL, EventsFileComparator.compare(f1, f2), "return val = "  + FILES_ARE_EQUAL);

		assertEquals(FILES_ARE_EQUAL, EventsFileComparator.compare(f2, f1), "return val = "  + FILES_ARE_EQUAL);
	}

	@Test
	void testRetCodeM1() {
		String f1 = utils.getClassInputDirectory() + "/events0.xml.gz";
		String f2 = utils.getClassInputDirectory() + "/events1.xml.gz";
		assertEquals(DIFFERENT_NUMBER_OF_TIMESTEPS, EventsFileComparator.compare(f1, f2), "return val " +DIFFERENT_NUMBER_OF_TIMESTEPS);

		assertEquals(DIFFERENT_NUMBER_OF_TIMESTEPS, EventsFileComparator.compare(f2, f1), "return val " +DIFFERENT_NUMBER_OF_TIMESTEPS);
	}

	@Test
	void testRetCodeM2() {
		String f1 = utils.getClassInputDirectory() + "/events0.xml.gz";
		String f2 = utils.getClassInputDirectory() + "/events2.xml.gz";
		assertEquals(DIFFERENT_TIMESTEPS, EventsFileComparator.compare(f1, f2), "return val = " + DIFFERENT_TIMESTEPS);

		assertEquals(DIFFERENT_TIMESTEPS, EventsFileComparator.compare(f2, f1), "return val = " + DIFFERENT_TIMESTEPS);
	}

	@Test
	void testRetCodeM3() {
		String f1 = utils.getClassInputDirectory() + "/events0.xml.gz";
		String f2 = utils.getClassInputDirectory() + "/events3.xml.gz";
		assertEquals(MISSING_EVENT, EventsFileComparator.compare(f1, f2), "return val = " + MISSING_EVENT);

		assertEquals(MISSING_EVENT, EventsFileComparator.compare(f2, f1), "return val = " + MISSING_EVENT);
	}

	@Test
	void testRetCodeM4() {
		String f1 = utils.getClassInputDirectory() + "/events0.xml.gz";
		String f2 = utils.getClassInputDirectory() + "/events4.xml.gz";
		assertEquals(WRONG_EVENT_COUNT, EventsFileComparator.compare(f1, f2), "return val = " + WRONG_EVENT_COUNT);

		assertEquals(WRONG_EVENT_COUNT, EventsFileComparator.compare(f2, f1), "return val = " + WRONG_EVENT_COUNT);
	}

	/** Missing file should lead to IO_ERROR (no hang). */
	@Test
	void testMissingFile_returnsIoError() throws IOException {
		// valid minimal events file on one side
		Path valid = Path.of(utils.getClassInputDirectory(), "valid.xml");
		write(valid, "<events></events>");

		// non-existent file on the other side
		Path missing = Path.of(utils.getClassInputDirectory(), "does-not-exist.xml");

		assertEquals(FILE_ERROR, EventsFileComparator.compare(missing.toString(), valid.toString()),
				"missing file should yield IO_ERROR");
		// also try flipped order
		assertEquals(FILE_ERROR, EventsFileComparator.compare(valid.toString(), missing.toString()),
				"missing file should yield IO_ERROR");
	}

	/** Empty/corrupted file should lead to IO_ERROR (no hang). */
	@Test
	void testCorruptedFile_returnsIoError() throws IOException {
		Path valid = Path.of(utils.getClassInputDirectory(), "valid2.xml");
		write(valid, "<events></events>");

		// empty file (could also write malformed XML like "<events>" to simulate corruption)
		Path corrupted = Path.of(utils.getClassInputDirectory(), "corrupted.xml");
		write(corrupted, "");

		assertEquals(FILE_ERROR, EventsFileComparator.compare(valid.toString(), corrupted.toString()),
				"empty/corrupted file should yield IO_ERROR");
		assertEquals(FILE_ERROR, EventsFileComparator.compare(corrupted.toString(), valid.toString()),
				"empty/corrupted file should yield IO_ERROR");
	}

	private static void write(Path p, String content) throws IOException {
		Files.createDirectories(p.getParent());
		Files.write(p, content.getBytes(StandardCharsets.UTF_8));
	}
}
