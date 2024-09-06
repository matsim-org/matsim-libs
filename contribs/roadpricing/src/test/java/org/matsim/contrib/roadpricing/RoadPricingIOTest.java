/* *********************************************************************** *
 * project: org.matsim.*
 * RoadPricingIOTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.contrib.roadpricing;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Tests Parsers and Writers for RoadPricingSchemes.
 *
 * @author mrieser
 */
public class RoadPricingIOTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	/**
	 * Tests reader and writer to ensure that reading and writing does not modify the schemes.
	 */
	@Test
	void testWriteReadWrite() {
		final String origFile = utils.getClassInputDirectory() + "roadpricing1.xml";
		final String tmpFile1 = utils.getOutputDirectory() + "roadpricing1.xml";
		final String tmpFile2 = utils.getOutputDirectory() + "roadpricing2.xml";

		final Id<Link> id1 = Id.create(1, Link.class);
		final Id<Link> id2 = Id.create(2, Link.class);
		final Id<Link> id3 = Id.create(3, Link.class);

		// first, read the scheme from file
		RoadPricingSchemeImpl scheme1 = RoadPricingUtils.addOrGetMutableRoadPricingScheme( ScenarioUtils.createScenario( ConfigUtils.createConfig() ) );
		RoadPricingReaderXMLv1 reader1 = new RoadPricingReaderXMLv1(scheme1);
		reader1.readFile(origFile);

		// compare it with what's expected
		assertEquals("distance-toll-1", scheme1.getName());
		assertEquals("distance toll for org.matsim.contrib.roadpricing.RoadPricingTestUtils.createNetwork1().", scheme1.getDescription());
		assertEquals(3, scheme1.getTolledLinkIds().size());
		assertTrue(scheme1.getTolledLinkIds().contains(id1));
		assertTrue(scheme1.getTolledLinkIds().contains(id2));
		assertTrue(scheme1.getTolledLinkIds().contains(id3));
		assertEquals(3, scheme1.getCostArray().length);
		Iterator<RoadPricingSchemeImpl.Cost> costIter = scheme1.getTypicalCosts().iterator();
		RoadPricingSchemeImpl.Cost cost = costIter.next();
		assertEquals(6*3600.0, cost.startTime, MatsimTestUtils.EPSILON);
		assertEquals(10*3600.0, cost.endTime, MatsimTestUtils.EPSILON);
		assertEquals(0.00020, cost.amount, MatsimTestUtils.EPSILON);
		cost = costIter.next();
		assertEquals(10*3600.0, cost.startTime, MatsimTestUtils.EPSILON);
		assertEquals(15*3600.0, cost.endTime, MatsimTestUtils.EPSILON);
		assertEquals(0.00010, cost.amount, MatsimTestUtils.EPSILON);
		cost = costIter.next();
		assertEquals(15*3600.0, cost.startTime, MatsimTestUtils.EPSILON);
		assertEquals(19*3600.0, cost.endTime, MatsimTestUtils.EPSILON);
		assertEquals(0.00020, cost.amount, MatsimTestUtils.EPSILON);
		assertFalse(costIter.hasNext());
		assertTrue(scheme1.getTypicalCostsForLink().containsKey(id1));
		assertNull(scheme1.getTypicalCostsForLink().get(id1));
		assertTrue(scheme1.getTypicalCostsForLink().containsKey(id2));
		assertNull(scheme1.getTypicalCostsForLink().get(id2));
		assertTrue(scheme1.getTypicalCostsForLink().containsKey(id3));
		assertNotNull(scheme1.getTypicalCostsForLink().get(id3));

		// write the scheme to a file
		RoadPricingWriterXMLv1 writer1 = new RoadPricingWriterXMLv1(scheme1);
		writer1.writeFile(tmpFile1);
		assertTrue(new File(tmpFile1).length() > 0); // make sure the file is not empty

		/* we cannot yet compare the written file with the original file, as the
		 * original file may have be edited manually and may have other indentation
		 * than the written one. Thus, read this file again and write it again and
		 * compare them.
		 */
		RoadPricingSchemeImpl scheme2 = RoadPricingUtils.addOrGetMutableRoadPricingScheme( ScenarioUtils.createScenario( ConfigUtils.createConfig() ) );
		RoadPricingReaderXMLv1 reader2 = new RoadPricingReaderXMLv1(scheme2);
		reader2.readFile(tmpFile1);

		// write the scheme to a file
		RoadPricingWriterXMLv1 writer2 = new RoadPricingWriterXMLv1(scheme2);
		writer2.writeFile(tmpFile2);

		// now compare the two files
		long cksum1 = CRCChecksum.getCRCFromFile(tmpFile1);
		long cksum2 = CRCChecksum.getCRCFromFile(tmpFile2);

		assertEquals(cksum1, cksum2);
	}
}
