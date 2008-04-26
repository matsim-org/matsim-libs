/* *********************************************************************** *
 * project: org.matsim.*
 * CountsReaderHandlerImplV1Test.java
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

public class CountsReaderHandlerImplV1Test extends MatsimTestCase {

	private AttributeFactory attributeFactory = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.attributeFactory = new AttributeFactory();
	}

	public void testSECounts() {
		final Counts counts = new Counts();
		CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(counts);
		reader.startTag("counts", this.attributeFactory.createCountsAttributes(), null);

		assertEquals("Counts attribute setting failed", "testName", counts.getName());
		assertEquals("Counts attribute setting failed", "testDesc", counts.getDescription());
		assertEquals("Counts attribute setting failed", 2000, counts.getYear());
		assertEquals("Counts attribute setting failed", "testLayer", counts.getLayer());
	}

	public void testSECount() {
		final Counts counts = new Counts();
		CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(counts);

		reader.startTag("counts", this.attributeFactory.createCountsAttributes(), null);
		reader.startTag("count", this.attributeFactory.createCountAttributes(), null);

		assertEquals("Count attribute setting failed", "testNr", counts.getCount(new IdImpl(1)).getCsId());
	}

	public void testSEVolume() {
		final Counts counts = new Counts();
		CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(counts);
		reader.startTag("counts", this.attributeFactory.createCountsAttributes(), null);
		reader.startTag("count", this.attributeFactory.createCountAttributes(), null);
		reader.startTag("volume", this.attributeFactory.createVolumeAttributes(), null);

		assertEquals("Volume attribute setting failed", 100.0, counts.getCount(new IdImpl(1)).getVolume(1).getValue(), EPSILON);
	}
}
