/* *********************************************************************** *
 * project: org.matsim.*
 * PtCountsSimRealPerHourGraphTest.java
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
import org.matsim.testcases.MatsimTestCase;

import playground.yu.counts.pt.PtCountsSimRealPerHourGraph;
import playground.yu.counts.pt.PtCountSimComparisonWriter.PtCountsType;

public class PtCountsSimRealPerHourGraphTest extends MatsimTestCase {

	@Test
	public void testCreatBoardChart() {
		PtCountsFixture fixture = new PtBoardCountsFixture();
		fixture.setUp();

		PtCountsSimRealPerHourGraph eg = new PtCountsSimRealPerHourGraph(
				fixture.ceateCountSimCompList(), 1, "testCreateChart",
				PtCountsType.Boarding);
		assertNotNull("No graph is created", eg.createChart(0));
	}

	@Test
	public void testCreatAlightChart() {
		PtCountsFixture fixture = new PtAlightCountsFixture();
		fixture.setUp();

		PtCountsSimRealPerHourGraph eg = new PtCountsSimRealPerHourGraph(
				fixture.ceateCountSimCompList(), 1, "testCreateChart",
				PtCountsType.Alighting);
		assertNotNull("No graph is created", eg.createChart(0));
	}

	@Test
	public void testCreatOccupancyChart() {
		PtCountsFixture fixture = new PtOccupancyCountsFixture();
		fixture.setUp();

		PtCountsSimRealPerHourGraph eg = new PtCountsSimRealPerHourGraph(
				fixture.ceateCountSimCompList(), 1, "testCreateChart",
				PtCountsType.Occupancy);
		assertNotNull("No graph is created", eg.createChart(0));
	}
}
