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
		CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(Counts.getSingleton());
		reader.startTag("counts", this.attributeFactory.createCountsAttributes(), null);

		assertTrue("Counts attribute setting failed", Counts.getSingleton().getName().equals("testName"));
		assertTrue("Counts attribute setting failed", Counts.getSingleton().getDescription().equals("testDesc"));
		assertTrue("Counts attribute setting failed", Counts.getSingleton().getYear()==2000);
		assertTrue("Counts attribute setting failed", Counts.getSingleton().getLayer().equals("testLayer"));
	}

	public void testSECount() {
		CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(Counts.getSingleton());

		reader.startTag("counts", this.attributeFactory.createCountsAttributes(), null);
		reader.startTag("count", this.attributeFactory.createCountAttributes(), null);

		assertTrue("Count attribute setting failed", Counts.getSingleton().getCount(new IdImpl(1)).getCsId().equals("testNr"));
	}

	public void testSEVolume() {
		CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(Counts.getSingleton());
		reader.startTag("counts", this.attributeFactory.createCountsAttributes(), null);
		reader.startTag("count", this.attributeFactory.createCountAttributes(), null);
		reader.startTag("volume", this.attributeFactory.createVolumeAttributes(), null);

		assertTrue("Volume attribute setting failed", Counts.getSingleton().getCount(new IdImpl(1)).getVolume(1).getValue()==100.0);

	}
}
