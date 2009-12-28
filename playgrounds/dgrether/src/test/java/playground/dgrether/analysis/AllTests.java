/* *********************************************************************** *
 * project: org.matsim.*
 * AllTests
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * @author dgrether
 *
 */
public abstract class AllTests {
	
	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for " + AllTests.class.getPackage().getName());
		suite.addTestSuite(DgAnalysisTest.class);
		suite.addTestSuite(DgGridTest.class);
		return suite;
	}
}
