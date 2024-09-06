/* *********************************************************************** *
 * project: org.matsim.*
 * CountsParserTest.java
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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.testcases.MatsimTestUtils;
import org.xml.sax.SAXException;

public class CountsParserTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testSEElementCounts() throws SAXException {
		AttributeFactory attributeFactory = new AttributeFactory();
		final Counts counts = new Counts();
		MatsimCountsReader reader = new MatsimCountsReader(counts);
		reader.setDoctype("counts_v1.xsd");

		reader.startElement("", "counts", "counts", attributeFactory.createCountsAttributes());

		assertEquals("testName", counts.getName(), "Counts attribute setting failed");
		assertEquals("testDesc", counts.getDescription(), "Counts attribute setting failed");
		assertEquals(2000, counts.getYear(), "Counts attribute setting failed");
		try {
			reader.endElement("", "counts", "counts");
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void testSEElementCountWithoutCoords() throws SAXException {
		AttributeFactory attributeFactory = new AttributeFactory();
		final Counts counts = new Counts();
		MatsimCountsReader reader = new MatsimCountsReader(counts);
		reader.setDoctype("counts_v1.xsd");

		reader.startElement("", "counts", "counts", attributeFactory.createCountsAttributes());
		reader.startElement("", "count", "count", attributeFactory.createCountAttributes());

		Count count = counts.getCount(Id.create(1, Link.class));
		assertEquals("testNr", count.getCsLabel(), "Count attribute setting failed");
		assertNull(count.getCoord(), "Count attributes x,y should not be set");

		reader.endElement("", "count", "count");
		reader.endElement("", "counts", "counts");
	}

	@Test
	void testSEElementCountWithCoords() throws SAXException {
		AttributeFactory attributeFactory = new AttributeFactory();
		final Counts counts = new Counts();
		MatsimCountsReader reader = new MatsimCountsReader(counts);
		reader.setDoctype("counts_v1.xsd");

		reader.startElement("", "counts", "counts", attributeFactory.createCountsAttributes());
		reader.startElement("", "count", "count", attributeFactory.createCountAttributesWithCoords());

		Count count = counts.getCount(Id.create(1, Link.class));
		assertNotNull(count.getCoord(), "Count attribute x,y setting failed");
		assertEquals(123.456, count.getCoord().getX(), MatsimTestUtils.EPSILON, "Count attribute x setting failed");
		assertEquals(987.654, count.getCoord().getY(), MatsimTestUtils.EPSILON, "Count attribute y setting failed");

		reader.endElement("", "count", "count");
		reader.endElement("", "counts", "counts");
	}

	@Test
	void testSEElementVolume() throws SAXException {
		AttributeFactory attributeFactory = new AttributeFactory();
		final Counts counts = new Counts();
		MatsimCountsReader reader = new MatsimCountsReader(counts);
		reader.setDoctype("counts_v1.xsd");

		reader.startElement("", "counts", "counts", attributeFactory.createCountsAttributes());
		reader.startElement("", "count", "count", attributeFactory.createCountAttributes());
		reader.startElement("", "volume", "volume", attributeFactory.createVolumeAttributes());

		assertEquals(100.0, counts.getCount(Id.create(1, Link.class)).getVolume(1).getValue(), MatsimTestUtils.EPSILON, "Volume attribute setting failed");

		reader.endElement("", "volume", "volume");
		reader.endElement("", "count", "count");
		reader.endElement("", "counts", "counts");
	}
}
