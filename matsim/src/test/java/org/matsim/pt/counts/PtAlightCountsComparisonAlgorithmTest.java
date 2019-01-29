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

package org.matsim.pt.counts;

import java.util.List;

import org.junit.Test;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.testcases.MatsimTestCase;


public class PtAlightCountsComparisonAlgorithmTest extends MatsimTestCase {

	@Test
	public void testCompare() {
		PtCountsFixture fixture = new PtAlightCountsFixture();
		fixture.setUp();

		CountsComparisonAlgorithm cca = fixture.getCCA();
		cca.run();

		List<CountSimComparison> csc_list = cca.getComparison();

		int cnt = 0;
		for (CountSimComparison csc : csc_list) {
			if (cnt != 8 && cnt!=32) {
				assertEquals("Wrong sim value set", 0d, csc.getSimulationValue(), 0d);
			} else if(cnt==8) {
				assertEquals("Wrong sim value set", 500d, csc.getSimulationValue(), 0d);
			}else{
				assertEquals("Wrong sim value set", 150d, csc.getSimulationValue(), 0d);
			}
			cnt++;
		}
	}

	@Test
	public void testDistanceFilter() {
		final PtCountsFixture fixture = new PtAlightCountsFixture();
		fixture.setUp();
		CountsComparisonAlgorithm cca = fixture.getCCA();

		cca.setCountCoordUsingDistanceFilter(4000.0, "11");
		cca.run();

		List<CountSimComparison> csc_list = cca.getComparison();
		assertEquals("Distance filter not working", 24, csc_list.size());
	}
}
