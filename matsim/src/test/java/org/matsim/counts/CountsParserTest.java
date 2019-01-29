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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;

public class CountsParserTest extends MatsimTestCase {

	public void testSEElementCounts() throws SAXException {
		AttributeFactory attributeFactory = new AttributeFactory();
		final Counts counts = new Counts();
		MatsimCountsReader reader = new MatsimCountsReader(counts);
		reader.setDoctype("counts_v1.xsd");

		reader.startElement("", "counts", "counts", attributeFactory.createCountsAttributes());

		assertEquals("Counts attribute setting failed", "testName", counts.getName());
		assertEquals("Counts attribute setting failed", "testDesc", counts.getDescription());
		assertEquals("Counts attribute setting failed", 2000, counts.getYear());
		try {
			reader.endElement("", "counts", "counts");
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
	}

	public void testSEElementCountWithoutCoords() throws SAXException {
		AttributeFactory attributeFactory = new AttributeFactory();
		final Counts counts = new Counts();
		MatsimCountsReader reader = new MatsimCountsReader(counts);
		reader.setDoctype("counts_v1.xsd");

		reader.startElement("", "counts", "counts", attributeFactory.createCountsAttributes());
		reader.startElement("", "count", "count", attributeFactory.createCountAttributes());

		Count count = counts.getCount(Id.create(1, Link.class));
		assertEquals("Count attribute setting failed", "testNr", count.getCsLabel());
		assertNull("Count attributes x,y should not be set", count.getCoord());

		reader.endElement("", "count", "count");
		reader.endElement("", "counts", "counts");
	}

	public void testSEElementCountWithCoords() throws SAXException {
		AttributeFactory attributeFactory = new AttributeFactory();
		final Counts counts = new Counts();
		MatsimCountsReader reader = new MatsimCountsReader(counts);
		reader.setDoctype("counts_v1.xsd");

		reader.startElement("", "counts", "counts", attributeFactory.createCountsAttributes());
		reader.startElement("", "count", "count", attributeFactory.createCountAttributesWithCoords());

		Count count = counts.getCount(Id.create(1, Link.class));
		assertNotNull("Count attribute x,y setting failed", count.getCoord());
		assertEquals("Count attribute x setting failed", 123.456, count.getCoord().getX(), EPSILON);
		assertEquals("Count attribute y setting failed", 987.654, count.getCoord().getY(), EPSILON);

		reader.endElement("", "count", "count");
		reader.endElement("", "counts", "counts");
	}

	public void testSEElementVolume() throws SAXException {
		AttributeFactory attributeFactory = new AttributeFactory();
		final Counts counts = new Counts();
		MatsimCountsReader reader = new MatsimCountsReader(counts);
		reader.setDoctype("counts_v1.xsd");

		reader.startElement("", "counts", "counts", attributeFactory.createCountsAttributes());
		reader.startElement("", "count", "count", attributeFactory.createCountAttributes());
		reader.startElement("", "volume", "volume", attributeFactory.createVolumeAttributes());

		assertEquals("Volume attribute setting failed", 100.0, counts.getCount(Id.create(1, Link.class)).getVolume(1).getValue(), EPSILON);

		reader.endElement("", "volume", "volume");
		reader.endElement("", "count", "count");
		reader.endElement("", "counts", "counts");
	}
}
