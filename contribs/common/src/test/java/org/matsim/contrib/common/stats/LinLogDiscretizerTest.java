/* *********************************************************************** *
 * project: org.matsim.*
 * LinLogDiscretizerTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.common.stats;

import junit.framework.TestCase;

/**
 * @author illenberger
 *
 */
public class LinLogDiscretizerTest extends TestCase {

	public void test() {
		Discretizer d = new LinLogDiscretizer(1000.0, 2.0);
		
		assertEquals(1000.0, d.discretize(500));
		assertEquals(1000.0, d.discretize(1000.0));
		assertEquals(2000.0, d.discretize(1200.0));
		assertEquals(2000.0, d.discretize(2000.0));
		assertEquals(4000.0, d.discretize(2100.0));
		assertEquals(128000.0, d.discretize(84651.54));
		
		assertEquals(1000.0, d.binWidth(-1));
		assertEquals(1000.0, d.binWidth(0));
		assertEquals(1000.0, d.binWidth(30));
		assertEquals(1000.0, d.binWidth(999));
		assertEquals(1000.0, d.binWidth(1000));
		assertEquals(1000.0, d.binWidth(1400));
		assertEquals(1000.0, d.binWidth(2000));
		assertEquals(2000.0, d.binWidth(2100));
		assertEquals(2000.0, d.binWidth(4000));
		assertEquals(4000.0, d.binWidth(4100));
		assertEquals(64000.0, d.binWidth(84651.54));
	}
}
