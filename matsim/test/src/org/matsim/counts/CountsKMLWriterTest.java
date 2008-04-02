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

import org.matsim.counts.Counts;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.transformations.TransformationFactory;

/**
 * Tests if some graphs are created.
 * As types of graphs can be plugged in or removed no testing for the existence of particular graphs is done.
 */
public class CountsKMLWriterTest extends MatsimTestCase {

	private CountsFixture fixture = null;

	public CountsKMLWriterTest() {
		this.fixture = new CountsFixture();
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.fixture.setUp();
	}

	public void testKMLCreation() {

		CountsComparisonAlgorithm cca=this.fixture.getCCA();
		cca.run(Counts.getSingleton());
		
		String filename = "test/output/org/matsim/counts/CountsKMLWriterTest/testKMLCreation/countscompare.kmz";
		CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(
				cca.getComparison(), this.fixture.getNetwork(), TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84 ));
		kmlWriter.setIterationNumber(0);
		kmlWriter.write(filename);

		assertTrue(!filename.isEmpty());
	}
}
