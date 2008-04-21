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

import org.matsim.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;

public class CountsParserTest extends MatsimTestCase {

	private AttributeFactory attributeFactory = null;

	public CountsParserTest() {
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.attributeFactory = new AttributeFactory();
	}

	public void testSEElementCounts() {
		MatsimCountsReader reader = new MatsimCountsReader(Counts.getSingleton());
		reader.setDoctype("counts_v1.xsd");

		try {
			reader.startElement("", "counts", "counts", this.attributeFactory.createCountsAttributes());
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}

		assertTrue("Counts attribute setting failed",Counts.getSingleton().getName().equals("testName"));
		assertTrue("Counts attribute setting failed",Counts.getSingleton().getDescription().equals("testDesc"));
		assertTrue("Counts attribute setting failed",Counts.getSingleton().getYear()==2000);
		assertTrue("Counts attribute setting failed",Counts.getSingleton().getLayer().equals("testLayer"));
		try {
			reader.endElement("", "counts", "counts");
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
	}

	public void testSEElementCountWithoutCoords() {
		MatsimCountsReader reader = new MatsimCountsReader(Counts.getSingleton());
		reader.setDoctype("counts_v1.xsd");

		try {
			reader.startElement("", "counts", "counts", this.attributeFactory.createCountsAttributes());
			reader.startElement("", "count", "count", this.attributeFactory.createCountAttributes());
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}

		Count count = Counts.getSingleton().getCount(new IdImpl(1));
		assertTrue("Count attribute setting failed", count.getCsId().equals("testNr"));
		assertNull("Count attributes x,y should not be set", count.getCoord());

		try {
			reader.endElement("", "count", "count");
			reader.endElement("", "counts", "counts");
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}

	}

	public void testSEElementCountWithCoords() {
		MatsimCountsReader reader = new MatsimCountsReader(Counts.getSingleton());
		reader.setDoctype("counts_v1.xsd");

		try {
			reader.startElement("", "counts", "counts", this.attributeFactory.createCountsAttributes());
			reader.startElement("", "count", "count", this.attributeFactory.createCountAttributesWithCoords());
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}

		Count count = Counts.getSingleton().getCount(new IdImpl(1));
		assertNotNull("Count attribute x,y setting failed", count.getCoord());
		assertEquals("Count attribute x setting failed", 123.456, count.getCoord().getX(), 1e-9);
		assertEquals("Count attribute y setting failed", 987.654, count.getCoord().getY(), 1e-9);

		try {
			reader.endElement("", "count", "count");
			reader.endElement("", "counts", "counts");
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}

	}

	public void testSEElementVolume() {
		MatsimCountsReader reader = new MatsimCountsReader(Counts.getSingleton());
		reader.setDoctype("counts_v1.xsd");

		try {
			reader.startElement("", "counts", "counts", this.attributeFactory.createCountsAttributes());
			reader.startElement("", "count", "count", this.attributeFactory.createCountAttributes());
			reader.startElement("", "volume", "volume", this.attributeFactory.createVolumeAttributes());
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}

		assertTrue("Volume attribute setting failed",Counts.getSingleton().getCount(new IdImpl(1)).getVolume(1).getValue()==100.0);

		try {
			reader.endElement("", "volume", "volume");
			reader.endElement("", "count", "count");
			reader.endElement("", "counts", "counts");
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}

	}
}
