/* *********************************************************************** *
 * project: org.matsim.*
 * PtCountsLoadCurveGraphTest.java
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

import org.junit.Test;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraph;
import org.matsim.testcases.MatsimTestCase;

public class PtCountsLoadCurveGraphTest extends MatsimTestCase {
	@Test
	public void testCreateBoardChart() {
		PtCountsFixture fixture = new PtBoardCountsFixture();
		fixture.setUp();

		CountsLoadCurveGraph eg = new CountsLoadCurveGraph(fixture
				.ceateCountSimCompList(), 1, "testCreateChart");
		assertNotNull("No graph is created", eg.createChart(0));
	}

	@Test
	public void testCreateAlightChart() {
		PtCountsFixture fixture = new PtAlightCountsFixture();
		fixture.setUp();

		CountsLoadCurveGraph eg = new CountsLoadCurveGraph(fixture
				.ceateCountSimCompList(), 1, "testCreateChart");
		assertNotNull("No graph is created", eg.createChart(0));
	}

	@Test
	public void testCreateOccupancyChart() {
		PtCountsFixture fixture = new PtOccupancyCountsFixture();
		fixture.setUp();

		CountsLoadCurveGraph eg = new CountsLoadCurveGraph(fixture
				.ceateCountSimCompList(), 1, "testCreateChart");
		assertNotNull("No graph is created", eg.createChart(0));
	}
}
