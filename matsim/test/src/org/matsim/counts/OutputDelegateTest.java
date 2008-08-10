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

import java.io.File;
import java.util.List;
import java.util.Vector;

import org.matsim.basic.v01.IdImpl;
import org.matsim.counts.algorithms.graphs.CountsSimRealPerHourGraph;
import org.matsim.counts.algorithms.graphs.helper.OutputDelegate;
import org.matsim.counts.algorithms.graphs.helper.Section;
import org.matsim.testcases.MatsimTestCase;

public class OutputDelegateTest extends MatsimTestCase {

	public void testOutPutAll() {
		CountsFixture fixture = new CountsFixture();
		fixture.setUp();

		CountsSimRealPerHourGraph sg = null;
		List<CountSimComparison> countSimCompList=new Vector<CountSimComparison>();
		for (int i=0; i<24; i++) {
			countSimCompList.add(new CountSimComparisonImpl(new IdImpl(i+1), 1, 1.0, 1.0));
		}
		sg = new CountsSimRealPerHourGraph(countSimCompList, 1, "testOutPutAll");

		new File(getOutputDirectory() + "graphs").mkdir();
		new File(getOutputDirectory() + "graphs/pdf").mkdir();
		new File(getOutputDirectory() + "graphs/png").mkdir();
		OutputDelegate outputDelegate=new OutputDelegate(getOutputDirectory() + "graphs/");
		outputDelegate.addSection(new Section("testOutPutAll"));
		assertNotNull("No graph was created", sg.createChart(0));
		outputDelegate.addCountsGraph(sg);
		outputDelegate.outPutAll(true, true);

		String filename = getOutputDirectory() + "graphs/pdf/" + sg.getFilename() +".pdf";
		File fPdf = new File(filename);
		assertTrue("The pdf output file " + filename + " doesn't exist", fPdf.exists());
		assertTrue("The pdf output file " + filename + " is empty", fPdf.length()>0.0);

		filename = getOutputDirectory() + "graphs/png/" + sg.getFilename() +".png";
		File fPng = new File(filename);
		assertTrue("The png output file " + filename + " doesn't exist", fPng.exists());
		assertTrue("The png output file " + filename + " is empty", fPng.length()>0.0);
	}
}
