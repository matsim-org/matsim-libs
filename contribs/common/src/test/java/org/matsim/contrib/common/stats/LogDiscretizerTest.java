/* *********************************************************************** *
 * project: org.matsim.*
 * LogDiscretizerTest.java
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
public class LogDiscretizerTest extends TestCase {

	public void test() {
		Discretizer d = new LogDiscretizer(2.0);
		
		assertEquals(1.0, d.discretize(-1.0));
		assertEquals(1.0, d.discretize(0.0));
		assertEquals(1.0, d.discretize(0.9));
		assertEquals(1.0, d.discretize(1.0));
		assertEquals(2.0, d.discretize(1.5));
		assertEquals(512.0, d.discretize(352.5));
		
		assertEquals(1.0, d.binWidth(-1.0));
		assertEquals(1.0, d.binWidth(0.0));
		assertEquals(1.0, d.binWidth(1.0));
		assertEquals(1.0, d.binWidth(1.5));
		assertEquals(2.0, d.binWidth(2.1));
		assertEquals(4.0, d.binWidth(5.0));
		assertEquals(4.0, d.binWidth(8.0));
		assertEquals(8.0, d.binWidth(8.1));
	}
}
