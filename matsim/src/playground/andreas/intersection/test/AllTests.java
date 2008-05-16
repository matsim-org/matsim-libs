package playground.andreas.intersection.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {

		TestSuite suite = new TestSuite("Tests for org.matsim.network");
		//$JUnit-BEGIN$
		suite.addTestSuite(TravelTimeTest2a.class);
		suite.addTestSuite(TravelTimeTest4a.class);
		suite.addTestSuite(CalculateAngleTest4a.class);
//		suite.addTest(org.matsim.network.algorithms.AllTests.suite());
		//$JUnit-END$
		return suite;
	}

}
