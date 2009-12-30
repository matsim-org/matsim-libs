package org.matsim.core.utils.geometry;



import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
	
	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for " + AllTests.class.getPackage().getName());
		//$JUnit-BEGIN$
		suite.addTest(org.matsim.core.utils.geometry.geotools.AllTests.suite());
		suite.addTest(org.matsim.core.utils.geometry.transformations.AllTests.suite());
		//$JUnit-END$
		return suite;
	}

}
