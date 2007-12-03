package org.matsim.utils.geometry;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
	
	public static Test suite() {
		TestSuite suite = new TestSuite("Test for org.matsim.utils.geometry");
		//$JUnit-BEGIN$
		suite.addTest(org.matsim.utils.geometry.shared.AllTests.suite());
		suite.addTest(org.matsim.utils.geometry.transformations.AllTests.suite());
		suite.addTestSuite(MGCTest.class);
		//$JUnit-END$
		return suite;
	}

}
