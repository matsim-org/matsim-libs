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

import java.io.File;
import java.util.Locale;

import org.matsim.counts.Counts;
import org.matsim.counts.algorithms.CountSimComparisonTableWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.testcases.MatsimTestCase;


public class CountsTableWriterTest extends MatsimTestCase {

	private CountsFixture fixture = null;

	public CountsTableWriterTest() {
		this.fixture = new CountsFixture();
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.fixture.setUp();
	}

	public void testTableCreation() {
		CountsComparisonAlgorithm cca = this.fixture.getCCA();
		cca.run(Counts.getSingleton());

		CountSimComparisonTableWriter ctw = new CountSimComparisonTableWriter(cca.getComparison(), Locale.ENGLISH);
		ctw.write(this.getOutputDirectory() + "/countTable.txt");

		File f = new File(this.getOutputDirectory() + "/countTable.txt");
		assertTrue(f.length() > 0.0);
	}
}
