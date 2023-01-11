/* *********************************************************************** *
 * project: org.matsim.*
 * CountsLoadCurveGraphTest.java
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

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraph;
import org.matsim.testcases.MatsimTestUtils;

public class CountsLoadCurveGraphTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();


	@Test public void testCreateChart() {
		CountsFixture fixture = new CountsFixture();
		fixture.setUp();

		CountsLoadCurveGraph eg = new CountsLoadCurveGraph(fixture.ceateCountSimCompList(), 1, "testCreateChart");
		assertNotNull("No graph is created", eg.createChart(0));
	}
}
