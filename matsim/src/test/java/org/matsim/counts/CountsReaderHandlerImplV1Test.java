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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.testcases.MatsimTestUtils;

public class CountsReaderHandlerImplV1Test {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testSECounts() {
		AttributeFactory attributeFactory = new AttributeFactory();
		final Counts counts = new Counts();
		CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(counts);
		reader.startTag("counts", attributeFactory.createCountsAttributes(), null);

		assertEquals("testName", counts.getName(), "Counts attribute setting failed");
		assertEquals("testDesc", counts.getDescription(), "Counts attribute setting failed");
		assertEquals(2000, counts.getYear(), "Counts attribute setting failed");
	}

	@Test
	void testSECount() {
		AttributeFactory attributeFactory = new AttributeFactory();
		final Counts counts = new Counts();
		CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(counts);

		reader.startTag("counts", attributeFactory.createCountsAttributes(), null);
		reader.startTag("count", attributeFactory.createCountAttributes(), null);

		assertEquals("testNr", counts.getCount(Id.create(1, Link.class)).getCsLabel(), "Count attribute setting failed");
	}

	@Test
	void testSEVolume() {
		AttributeFactory attributeFactory = new AttributeFactory();
		final Counts counts = new Counts();
		CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(counts);
		reader.startTag("counts", attributeFactory.createCountsAttributes(), null);
		reader.startTag("count", attributeFactory.createCountAttributes(), null);
		reader.startTag("volume", attributeFactory.createVolumeAttributes(), null);

		assertEquals(100.0, counts.getCount(Id.create(1, Link.class)).getVolume(1).getValue(), MatsimTestUtils.EPSILON, "Volume attribute setting failed");
	}
}
