
/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimCountsIOTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author mrieser / Simunto GmbH
 */
public class MatsimCountsIOTest {

	/**
	 * Such a file with year 0 could be created by MATSim when the year was not explicitly set,
	 * but it could not be read back in as "0" was not recognized as a valid year by the XML validator originally.
	 */
	@Test
	public void testReading_year0() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<counts xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"xsi:noNamespaceSchemaLocation=\"http://matsim.org/files/dtd/counts_v1.xsd\"\n" +
				" name=\"test\" desc=\"test counting stations\" year=\"0\"  layer=\"0\" \n" +
				" > \n" +
				"\n" +
				"\t<count loc_id=\"100\" cs_id=\"005\">\n" +
				"\t\t<volume h=\"1\" val=\"10.00\" />\n" +
				"\t\t<volume h=\"2\" val=\"1.00\" />\n" +
				"\t\t<volume h=\"3\" val=\"2.00\" />\n" +
				"\t\t<volume h=\"4\" val=\"3.00\" />\n" +
				"\t\t<volume h=\"5\" val=\"4.00\" />\n" +
				"\t\t<volume h=\"6\" val=\"5.00\" />\n" +
				"\t\t<volume h=\"7\" val=\"6.00\" />\n" +
				"\t\t<volume h=\"8\" val=\"7.00\" />\n" +
				"\t\t<volume h=\"9\" val=\"8.00\" />\n" +
				"\t\t<volume h=\"10\" val=\"9.00\" />\n" +
				"\t\t<volume h=\"11\" val=\"10.00\" />\n" +
				"\t\t<volume h=\"12\" val=\"11.00\" />\n" +
				"\t\t<volume h=\"13\" val=\"12.00\" />\n" +
				"\t\t<volume h=\"14\" val=\"13.00\" />\n" +
				"\t\t<volume h=\"15\" val=\"14.00\" />\n" +
				"\t\t<volume h=\"16\" val=\"15.00\" />\n" +
				"\t\t<volume h=\"17\" val=\"16.00\" />\n" +
				"\t\t<volume h=\"18\" val=\"17.00\" />\n" +
				"\t\t<volume h=\"19\" val=\"18.00\" />\n" +
				"\t\t<volume h=\"20\" val=\"19.00\" />\n" +
				"\t\t<volume h=\"21\" val=\"20.00\" />\n" +
				"\t\t<volume h=\"22\" val=\"21.00\" />\n" +
				"\t\t<volume h=\"23\" val=\"22.00\" />\n" +
				"\t\t<volume h=\"24\" val=\"23.00\" />\n" +
				"\t</count>\n" +
				"</counts>\n";

		Counts counts = new Counts();
		MatsimCountsReader reader = new MatsimCountsReader(counts);
		ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
		reader.parse(stream);
		Assert.assertEquals(1, counts.getCounts().size());
	}

	/**
	 * originally, year was defined of type "gYear" in the xml schema, which required 4-digit years.
	 * This test checks that such years can still be read in now that it is defined as int.
	 */
	@Test
	public void testReading_year1padded() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<counts xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"xsi:noNamespaceSchemaLocation=\"http://matsim.org/files/dtd/counts_v1.xsd\"\n" +
				" name=\"test\" desc=\"test counting stations\" year=\"0001\"  layer=\"0\" \n" +
				" > \n" +
				"\n" +
				"\t<count loc_id=\"100\" cs_id=\"005\">\n" +
				"\t\t<volume h=\"1\" val=\"10.00\" />\n" +
				"\t\t<volume h=\"2\" val=\"1.00\" />\n" +
				"\t\t<volume h=\"3\" val=\"2.00\" />\n" +
				"\t\t<volume h=\"4\" val=\"3.00\" />\n" +
				"\t\t<volume h=\"5\" val=\"4.00\" />\n" +
				"\t\t<volume h=\"6\" val=\"5.00\" />\n" +
				"\t\t<volume h=\"7\" val=\"6.00\" />\n" +
				"\t\t<volume h=\"8\" val=\"7.00\" />\n" +
				"\t\t<volume h=\"9\" val=\"8.00\" />\n" +
				"\t\t<volume h=\"10\" val=\"9.00\" />\n" +
				"\t\t<volume h=\"11\" val=\"10.00\" />\n" +
				"\t\t<volume h=\"12\" val=\"11.00\" />\n" +
				"\t\t<volume h=\"13\" val=\"12.00\" />\n" +
				"\t\t<volume h=\"14\" val=\"13.00\" />\n" +
				"\t\t<volume h=\"15\" val=\"14.00\" />\n" +
				"\t\t<volume h=\"16\" val=\"15.00\" />\n" +
				"\t\t<volume h=\"17\" val=\"16.00\" />\n" +
				"\t\t<volume h=\"18\" val=\"17.00\" />\n" +
				"\t\t<volume h=\"19\" val=\"18.00\" />\n" +
				"\t\t<volume h=\"20\" val=\"19.00\" />\n" +
				"\t\t<volume h=\"21\" val=\"20.00\" />\n" +
				"\t\t<volume h=\"22\" val=\"21.00\" />\n" +
				"\t\t<volume h=\"23\" val=\"22.00\" />\n" +
				"\t\t<volume h=\"24\" val=\"23.00\" />\n" +
				"\t</count>\n" +
				"</counts>\n";

		Counts counts = new Counts();
		MatsimCountsReader reader = new MatsimCountsReader(counts);
		ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
		reader.parse(stream);
		Assert.assertEquals(1, counts.getCounts().size());
	}

	@Test
	public void testDefaultYear_empty() {
		Counts counts = new Counts();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new CountsWriterV1(counts).write(out);

		Counts counts2 = new Counts();
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		new MatsimCountsReader(counts2).parse(in);
		// there should not have been an Exception
	}

}
