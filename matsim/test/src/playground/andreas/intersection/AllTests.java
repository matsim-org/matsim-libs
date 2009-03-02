package playground.andreas.intersection;

import org.matsim.signalsystems.CalculateAngleTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {

		TestSuite suite = new TestSuite("Tests for org.matsim.network");
		//$JUnit-BEGIN$
		suite.addTestSuite(CompareQSimQueueSim.class);
		suite.addTestSuite(TravelTimeTestFourWay.class);
		suite.addTestSuite(CalculateAngleTest.class);
		suite.addTestSuite(TravelTimeTestOneWay.class);
//		suite.addTest(org.matsim.network.algorithms.AllTests.suite());
		//$JUnit-END$
		return suite;
	}

}
