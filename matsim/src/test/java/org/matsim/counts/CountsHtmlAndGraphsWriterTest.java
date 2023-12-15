/* *********************************************************************** *
 * project: org.matsim.*
 * CountsGraphWriterTest.java
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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.counts.algorithms.CountsHtmlAndGraphsWriter;
import org.matsim.counts.algorithms.graphs.CountsErrorGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimReal24GraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimRealPerHourGraphCreator;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Tests if some graphs are created.
 * As types of graphs can be plugged in or removed no testing for the existence of particular graphs is done.
 */
public class CountsHtmlAndGraphsWriterTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testGraphCreation() {
		CountsFixture fixture = new CountsFixture();
		fixture.setUp();

		CountsComparisonAlgorithm cca = fixture.getCCA();
		cca.run();

		CountsHtmlAndGraphsWriter cgw = new CountsHtmlAndGraphsWriter(utils.getOutputDirectory(), cca.getComparison(),1);
		cgw.addGraphsCreator(new CountsSimRealPerHourGraphCreator("sim vs. real volumes per hour"));
		cgw.addGraphsCreator(new CountsErrorGraphCreator("Error Plots"));
		cgw.addGraphsCreator(new CountsLoadCurveGraphCreator("Load curve graph"));
		cgw.addGraphsCreator(new CountsSimReal24GraphCreator("average working day sim and count volumes"));
		cgw.createHtmlAndGraphs();

		assertTrue(cgw.getOutput().getGraphs().size()>0);
	}
}
