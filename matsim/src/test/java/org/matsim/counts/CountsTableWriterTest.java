/* *********************************************************************** *
 * project: org.matsim.*
 * CountsTableWriterTest.java
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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Locale;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.counts.algorithms.CountSimComparisonTableWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.testcases.MatsimTestUtils;


public class CountsTableWriterTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();


	@Test public void testTableCreation() {
		CountsFixture fixture = new CountsFixture();
		fixture.setUp();

		CountsComparisonAlgorithm cca = fixture.getCCA();
		cca.run();

		CountSimComparisonTableWriter ctw = new CountSimComparisonTableWriter(cca.getComparison(), Locale.ENGLISH);
		ctw.writeFile(utils.getOutputDirectory() + "/countTable.txt");

		File f = new File(utils.getOutputDirectory() + "/countTable.txt");
		assertTrue(f.length() > 0.0);
	}
}
