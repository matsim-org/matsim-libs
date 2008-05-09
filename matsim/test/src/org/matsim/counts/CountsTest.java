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

import org.matsim.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

public class CountsTest extends MatsimTestCase {

	public void testGetCounts() {
		final Counts counts = new Counts();
		counts.createCount(new IdImpl(0), "1");
		assertEquals("Getting counts failed", 1, counts.getCounts().size());
	}
}
