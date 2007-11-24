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

import org.matsim.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;

/**
 * Tests if parsing and writing of the counts xml file is done correctly
 */
public class CountsParserWriterTest extends MatsimTestCase {

	private CountsFixture fixture = null;

	public CountsParserWriterTest() {
		this.fixture = new CountsFixture();
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.fixture.setUp();
	}

	public void testParserWriter() {

		// PARSER:-----------------------------------------------
		// Creation and parsing done in fixture_

		// test if required fields of schema are filled out:
		// Counts:
		assertNotNull(Counts.getSingleton().getCounts());
		assertTrue(Counts.getSingleton().getYear()>2000);
		assertNotNull(Counts.getSingleton().getLayer());
		assertNotNull(Counts.getSingleton().getName());

		// Count & Volume
		Iterator<Count> c_it = Counts.getSingleton().getCounts().values().iterator();
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

		Gbl.getConfig();
		String filename = this.getOutputDirectory() + "output_counts.xml";
		CountsWriter counts_writer = new CountsWriter(Counts.getSingleton(), filename);
		counts_writer.write();
		File f = new File(filename);
		assertTrue(f.length() > 0.0);
	}
}
