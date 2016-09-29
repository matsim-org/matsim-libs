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

package org.matsim.pt.counts;

import java.io.File;

import org.junit.Test;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.pt.counts.obsolete.PtCountSimComparisonKMLWriter;
import org.matsim.testcases.MatsimTestCase;

/**
 * Tests if some graphs are created. As types of graphs can be plugged in or
 * removed no testing for the existence of particular graphs is done.
 */
public class PtCountsKMLWriterTest extends MatsimTestCase {
	@Test
	public void testPtAlightKMLCreation() {
		PtCountsFixture alightFixture = new PtAlightCountsFixture();
		alightFixture.setUp();
		CountsComparisonAlgorithm ccaAlight = alightFixture.getCCA();
		ccaAlight.run();

		PtCountsFixture boardFixture = new PtBoardCountsFixture();
		boardFixture.setUp();
		CountsComparisonAlgorithm ccaBoard = boardFixture.getCCA();
		ccaBoard.run();

		PtCountsFixture occupancyFixture = new PtOccupancyCountsFixture();
		occupancyFixture.setUp();
		CountsComparisonAlgorithm ccaOccupancy = occupancyFixture.getCCA();
		ccaOccupancy.run();

		String filename = this.getOutputDirectory() + "ptCountsCompare.kmz";
		final CoordinateTransformation coordTransform = TransformationFactory.getCoordinateTransformation(boardFixture.config.global().getCoordinateSystem(),	TransformationFactory.WGS84);
		{
			PtCountSimComparisonKMLWriter kmlWriter = new PtCountSimComparisonKMLWriter(ccaBoard.getComparison(), 
					ccaAlight.getComparison(),ccaOccupancy.getComparison(), 
					coordTransform,
					boardFixture.counts, alightFixture.counts,	occupancyFixture.counts);
			kmlWriter.setIterationNumber(0);
			kmlWriter.writeFile(filename);
		}
		{
			CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(ccaOccupancy.getComparison(), 
					occupancyFixture.counts, coordTransform, "ptCountsOccup") ;
			kmlWriter.setIterationNumber(0);
			kmlWriter.writeFile(filename);
		}

		assertTrue(new File(filename).length() > 0);
	}
}
