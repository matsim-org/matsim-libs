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
import java.io.IOException;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;
import org.xml.sax.SAXException;

/**
 * Tests if parsing and writing of the counts xml file is done correctly
 *
 * @author ahorni
 */
public class CountsParserWriterTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * @author ahorni
	 */
	@Test
	public void testParserWriter() {
		CountsFixture fixture = new CountsFixture();
		fixture.setUp();

		// PARSER:-----------------------------------------------
		// Creation and parsing done in fixture_

		// test if required fields of schema are filled out:
		// Counts:
		Assert.assertNotNull(fixture.counts.getCounts());
		Assert.assertTrue(fixture.counts.getYear()>2000);
		Assert.assertNotNull(fixture.counts.getName());

		// Count & Volume
		Iterator<Count> c_it = fixture.counts.getCounts().values().iterator();
		while (c_it.hasNext()) {
			Count c = c_it.next();
			Assert.assertNotNull(c.getId());

			Iterator<Volume> vol_it = c.getVolumes().values().iterator();
			while (vol_it.hasNext()) {
				Volume v = vol_it.next();
				Assert.assertTrue(v.getHourOfDayStartingWithOne()>0);
				Assert.assertTrue(v.getValue()>=0.0);
			}//while
		}//while

		// WRITER: -----------------------------------------------
		// what to test?
		// check if xml file not empty

		String filename = this.utils.getOutputDirectory() + "output_counts.xml";
		CountsWriter counts_writer = new CountsWriter(fixture.counts);
		counts_writer.write(filename);
		File f = new File(filename);
		Assert.assertTrue(f.length() > 0.0);
	}

	/**
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 *
	 * @author mrieser
	 */
	@Test
	public void testWriteParse_nameIsNull() throws SAXException, ParserConfigurationException, IOException {
		CountsFixture f = new CountsFixture();
		f.setUp();
		f.counts.setName(null);
		Assert.assertNull(f.counts.getName());
		String filename = this.utils.getOutputDirectory() + "counts.xml";
		new CountsWriterV1(f.counts).write(filename);

		Counts counts2 = new Counts();
		new CountsReaderMatsimV1(counts2).readFile(filename);
		Assert.assertEquals("", counts2.getName());
	}
}
