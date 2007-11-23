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

import org.matsim.counts.CountSimComparison;
import org.matsim.counts.Counts;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.testcases.MatsimTestCase;

public class CountsComparisonAlgorithmTest extends MatsimTestCase {

	private CountsFixture fixture = null;

	public CountsComparisonAlgorithmTest() {
		this.fixture = new CountsFixture();
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.fixture.setUp();
	}

	public void testCompare() {
		CountsComparisonAlgorithm cca = this.fixture.getCCA();
		cca.run(Counts.getSingleton());

		List<CountSimComparison> csc_list = cca.getComparison();

		int cnt=0;
		for (CountSimComparison csc : csc_list) {
			assertEquals("Wrong sim value set", 2*cnt, csc.getSimulationValue(), 0.0);
			cnt++;
		}//while
	}

	public void testDistanceFilter() {
		CountsComparisonAlgorithm cca = this.fixture.getCCA();
		cca.setDistanceFilter(Double.valueOf(0.5), "0");
		cca.run(Counts.getSingleton());

		List<CountSimComparison> csc_list = cca.getComparison();
		assertEquals("Distance filter not working", 0, csc_list.size());
	}

}
