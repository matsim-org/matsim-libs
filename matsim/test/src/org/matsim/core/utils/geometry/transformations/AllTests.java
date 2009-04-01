package org.matsim.core.utils.geometry.transformations;

import junit.framework.Test;
import junit.framework.TestSuite;


public class AllTests {
	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for " + AllTests.class.getPackage().getName());
		suite.addTestSuite(GeotoolsTransformationTest.class);
		suite.addTestSuite(TransformationFactoryTest.class);
		return suite;
	}
}
