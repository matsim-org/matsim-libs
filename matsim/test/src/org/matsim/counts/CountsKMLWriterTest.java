/* *********************************************************************** *
 * project: org.matsim.*
 * CountsKMLWriterTest.java
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

import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.testcases.MatsimTestCase;

/**
 * Tests if some graphs are created.
 * As types of graphs can be plugged in or removed no testing for the existence of particular graphs is done.
 */
public class CountsKMLWriterTest extends MatsimTestCase {

	public void testKMLCreation() {
		CountsFixture fixture = new CountsFixture();
		fixture.setUp();

		CountsComparisonAlgorithm cca=fixture.getCCA();
		cca.run();

		String filename = this.getOutputDirectory() + "countscompare.kmz";
		CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(
				cca.getComparison(), fixture.getNetwork(), new IdentityTransformation());
		kmlWriter.setIterationNumber(0);
		kmlWriter.writeFile(filename);

		assertTrue(new File(filename).length() > 0);
	}
}
