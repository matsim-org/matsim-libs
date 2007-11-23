/* *********************************************************************** *
 * project: org.matsim.*
 * CountsTest.java
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

import org.matsim.basic.v01.Id;
import org.matsim.testcases.MatsimTestCase;

public class CountsTest extends MatsimTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testAddAlgorithm() {
		MockAlgo algo=new MockAlgo();
		Counts.getSingleton().addAlgorithm(algo);
		assertTrue("Adding algorithm failed", Counts.getSingleton().getAlgorithms().size()==1);
	}

	public void testRunAlgorithms() {
		MockAlgo algo=new MockAlgo();
		Counts.getSingleton().addAlgorithm(algo);
		Counts.getSingleton().runAlgorithms();
		assertTrue("Running algorithms failed", Counts.getSingleton().getDescription().equals("SetByMock"));
	}

	public void testGetCounts() {
		Counts.getSingleton().createCount(new Id(0), "1");
		assertTrue("Getting counts failed", Counts.getSingleton().getCounts().size()==1);
	}
}
