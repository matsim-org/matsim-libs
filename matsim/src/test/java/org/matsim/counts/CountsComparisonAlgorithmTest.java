/* *********************************************************************** *
 * project: org.matsim.*
 * CountsComparisonAlgorithmTest.java
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

import java.util.List;

import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.testcases.MatsimTestCase;

public class CountsComparisonAlgorithmTest extends MatsimTestCase {

	public void testCompare() {
		CountsFixture fixture = new CountsFixture();
		fixture.setUp();

		CountsComparisonAlgorithm cca = fixture.getCCA();
		cca.run();

		List<CountSimComparison> csc_list = cca.getComparison();

		int cnt=0;
		for (CountSimComparison csc : csc_list) {
			assertEquals("Wrong sim value set", 2*cnt, csc.getSimulationValue(), 0.0);
			cnt++;
			cnt=cnt%24;
		}//while
	}

	public void testDistanceFilter() {
		CountsFixture fixture = new CountsFixture();
		fixture.setUp();

		CountsComparisonAlgorithm cca = fixture.getCCA();
		cca.setDistanceFilter(Double.valueOf(0.5), "1");
		cca.run();

		List<CountSimComparison> csc_list = cca.getComparison();
		assertEquals("Distance filter not working", 0, csc_list.size());
	}

}
