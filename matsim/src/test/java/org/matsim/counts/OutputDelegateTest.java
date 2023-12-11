/* *********************************************************************** *
 * project: org.matsim.*
 * OutputDelegateTest.java
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Vector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.algorithms.graphs.CountsSimRealPerHourGraph;
import org.matsim.counts.algorithms.graphs.helper.OutputDelegate;
import org.matsim.counts.algorithms.graphs.helper.Section;
import org.matsim.testcases.MatsimTestUtils;

public class OutputDelegateTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testOutputHtml() {
			CountsFixture fixture = new CountsFixture();
			fixture.setUp();

			CountsSimRealPerHourGraph sg = null;
			List<CountSimComparison> countSimCompList=new Vector<CountSimComparison>();
			for (int i=0; i<24; i++) {
				countSimCompList.add(new CountSimComparisonImpl(Id.create(i+1, Link.class), "", 1, 1.0, 1.0));
			}
			sg = new CountsSimRealPerHourGraph(countSimCompList, 1, "testOutPutAll");

		new File(utils.getOutputDirectory() + "graphs").mkdir();
		OutputDelegate outputDelegate=new OutputDelegate(utils.getOutputDirectory() + "graphs/");
			outputDelegate.addSection(new Section("testOutPutAll"));
			assertNotNull(sg.createChart(0), "No graph was created");
			outputDelegate.addCountsGraph(sg);
			outputDelegate.outputHtml();

		String filename = utils.getOutputDirectory() + "graphs/png/" + sg.getFilename() +".png";
			File fPng = new File(filename);
			assertTrue(fPng.exists(), "The png output file " + filename + " doesn't exist");
			assertTrue(fPng.length()>0.0, "The png output file " + filename + " is empty");
		}
}
