/* *********************************************************************** *
 * project: org.matsim.*
 * PtBoardCountsComparisonAlgorithmTest.java
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

import java.util.List;

import org.junit.Test;
import org.matsim.counts.CountSimComparison;
import org.matsim.testcases.MatsimTestCase;

import playground.yu.counts.pt.PtCountsComparisonAlgorithm;

public class PtBoardCountsComparisonAlgorithmTest extends MatsimTestCase {

	@Test
	public void testCompare() {
		PtCountsFixture fixture = new PtBoardCountsFixture();
		fixture.setUp();

		PtCountsComparisonAlgorithm cca = fixture.getCCA();
		cca.run();

		List<CountSimComparison> csc_list = cca.getComparison();

		int cnt = 0;
		for (CountSimComparison csc : csc_list) {
			if (cnt != 8) {
				assertEquals("Wrong sim value set", 0d, csc.getSimulationValue(), 0d);
			} else {
				assertEquals("Wrong sim value set", 650d, csc.getSimulationValue(), 0d);
			}
			cnt++;
		}
	}

	@Test
	public void testDistanceFilter() {
		PtCountsFixture fixture = new PtBoardCountsFixture();
		fixture.setUp();

		PtCountsComparisonAlgorithm cca = fixture.getCCA();
		cca.setDistanceFilter(Double.valueOf(5000), "11");
		cca.run();

		List<CountSimComparison> csc_list = cca.getComparison();
		assertEquals("Distance filter not working", 24, csc_list.size());
	}
}
