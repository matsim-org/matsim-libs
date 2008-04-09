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

package org.matsim.roadpricing;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.basic.v01.Id;
import org.matsim.network.NetworkLayer;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.xml.sax.SAXException;

/**
 * Tests Parsers and Writers for RoadPricingSchemes.
 *
 * @author mrieser
 */
public class RoadPricingIOTest extends MatsimTestCase {

	/**
	 * Tests reader and writer to ensure that reading and writing does not modify the schemes.
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public void testWriteReadWrite() throws SAXException, ParserConfigurationException, IOException {
		final String origFile = this.getClassInputDirectory() + "roadpricing1.xml";
		final String tmpFile1 = getOutputDirectory() + "roadpricing1.xml";
		final String tmpFile2 = getOutputDirectory() + "roadpricing2.xml";

		loadConfig(null);

		NetworkLayer network = Fixture.createNetwork1();
		// first, read the scheme from file
		RoadPricingReaderXMLv1 reader1 = new RoadPricingReaderXMLv1(network);
		reader1.parse(origFile);
		RoadPricingScheme scheme1 = reader1.getScheme();

		// compare it with what's expected
		assertEquals("distance-toll-1", scheme1.getName());
		assertEquals("distance toll for org.matsim.roadpricing.Fixture.createNetwork1().", scheme1.getDescription());
		assertEquals(3, scheme1.getLinks().size());
		assertTrue(scheme1.getLinkIds().contains(new Id(1)));
		assertTrue(scheme1.getLinkIds().contains(new Id(2)));
		assertTrue(scheme1.getLinkIds().contains(new Id(3)));
		assertEquals(3, scheme1.getCostArray().length);
		Iterator<RoadPricingScheme.Cost> costIter = scheme1.getCosts().iterator();
		RoadPricingScheme.Cost cost = costIter.next();
		assertEquals(6*3600.0, cost.startTime, EPSILON);
		assertEquals(10*3600.0, cost.endTime, EPSILON);
		assertEquals(0.00020, cost.amount, EPSILON);
		cost = costIter.next();
		assertEquals(10*3600.0, cost.startTime, EPSILON);
		assertEquals(15*3600.0, cost.endTime, EPSILON);
		assertEquals(0.00010, cost.amount, EPSILON);
		cost = costIter.next();
		assertEquals(15*3600.0, cost.startTime, EPSILON);
		assertEquals(19*3600.0, cost.endTime, EPSILON);
		assertEquals(0.00020, cost.amount, EPSILON);
		assertFalse(costIter.hasNext());

		// write the scheme to a file
		RoadPricingWriterXMLv1 writer1 = new RoadPricingWriterXMLv1(scheme1);
		writer1.writeFile(tmpFile1);
		assertTrue(new File(tmpFile1).length() > 0); // make sure the file is not empty

		/* we cannot yet compare the written file with the original file, as the
		 * original file may have be edited manually and may have other indentation
		 * than the written one. Thus, read this file again and write it again and
		 * compare them.
		 */

		RoadPricingReaderXMLv1 reader2 = new RoadPricingReaderXMLv1(network);
		reader2.parse(tmpFile1);
		RoadPricingScheme scheme2 = reader2.getScheme();

		// write the scheme to a file
		RoadPricingWriterXMLv1 writer2 = new RoadPricingWriterXMLv1(scheme2);
		writer2.writeFile(tmpFile2);

		// now compare the two files
		long cksum1 = CRCChecksum.getCRCFromFile(tmpFile1);
		long cksum2 = CRCChecksum.getCRCFromFile(tmpFile2);

		assertEquals(cksum1, cksum2);
	}
}
