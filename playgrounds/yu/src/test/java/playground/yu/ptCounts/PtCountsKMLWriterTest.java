/* *********************************************************************** *
 * project: org.matsim.*
 * PtCountsKMLWriterTest.java
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

package playground.yu.ptCounts;

import java.io.File;

import org.junit.Test;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.testcases.MatsimTestCase;

import playground.yu.counts.pt.PtCountSimComparisonKMLWriter;
import playground.yu.counts.pt.PtCountsComparisonAlgorithm;

/**
 * Tests if some graphs are created. As types of graphs can be plugged in or
 * removed no testing for the existence of particular graphs is done.
 */
public class PtCountsKMLWriterTest extends MatsimTestCase {
	@Test
	public void testPtAlightKMLCreation() {
		PtCountsFixture alightFixture = new PtAlightCountsFixture();
		alightFixture.setUp();
		PtCountsComparisonAlgorithm ccaAlight = alightFixture.getCCA();
		ccaAlight.run();

		PtCountsFixture boardFixture = new PtBoardCountsFixture();
		boardFixture.setUp();
		PtCountsComparisonAlgorithm ccaBoard = boardFixture.getCCA();
		ccaBoard.run();

		PtCountsFixture occupancyFixture = new PtOccupancyCountsFixture();
		occupancyFixture.setUp();
		PtCountsComparisonAlgorithm ccaOccupancy = occupancyFixture.getCCA();
		ccaOccupancy.run();

		String filename =
		// "../playgrounds/yu/" +
		this.getOutputDirectory() + "ptCountsCompare.kmz";
		PtCountSimComparisonKMLWriter kmlWriter = new PtCountSimComparisonKMLWriter(
				ccaBoard.getComparison(), ccaAlight.getComparison(),
				ccaOccupancy.getComparison(), TransformationFactory
						.getCoordinateTransformation(boardFixture.config
								.global().getCoordinateSystem(),
								TransformationFactory.WGS84),
				boardFixture.counts, alightFixture.counts,
				occupancyFixture.counts);
		kmlWriter.setIterationNumber(0);
		kmlWriter.writeFile(filename);

		assertTrue(new File(filename).length() > 0);
	}
}
