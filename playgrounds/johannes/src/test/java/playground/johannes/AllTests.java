/* *********************************************************************** *
 * project: org.matsim.*
 * AllTests.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author illenberger
 *
 */
public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for playground.johannes");

		suite.addTest(playground.johannes.graph.AllTests.suite());
		suite.addTest(playground.johannes.statistics.AllTests.suite());

		return suite;
	}

}
