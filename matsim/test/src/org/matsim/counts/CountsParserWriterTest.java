/* *********************************************************************** *
 * project: org.matsim.*
 * CountsParserWriterTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.counts;

import java.io.File;
import java.util.Iterator;

import org.matsim.testcases.MatsimTestCase;

/**
 * Tests if parsing and writing of the counts xml file is done correctly
 */
public class CountsParserWriterTest extends MatsimTestCase {

	public void testParserWriter() {
		CountsFixture fixture = new CountsFixture();
		fixture.setUp();

		// PARSER:-----------------------------------------------
		// Creation and parsing done in fixture_

		// test if required fields of schema are filled out:
		// Counts:
		assertNotNull(fixture.counts.getCounts());
		assertTrue(fixture.counts.getYear()>2000);
		assertNotNull(fixture.counts.getLayer());
		assertNotNull(fixture.counts.getName());

		// Count & Volume
		Iterator<Count> c_it = fixture.counts.getCounts().values().iterator();
		while (c_it.hasNext()) {
			Count c = c_it.next();
			assertNotNull(c.getLocId());

			Iterator<Volume> vol_it = c.getVolumes().values().iterator();
			while (vol_it.hasNext()) {
				Volume v = vol_it.next();
				assertTrue(v.getHour()>0);
				assertTrue(v.getValue()>=0.0);
			}//while
		}//while

		// WRITER: -----------------------------------------------
		// what to test?
		// check if xml file not empty

		String filename = this.getOutputDirectory() + "output_counts.xml";
		CountsWriter counts_writer = new CountsWriter(fixture.counts, filename);
		counts_writer.write();
		File f = new File(filename);
		assertTrue(f.length() > 0.0);
	}
}
