package playground.wrashid.PHEV.Utility;

import playground.wrashid.PHEV.Utility.EnergyConsumptionSamplesTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for " + AllTests.class.getPackage().getName());

		suite.addTestSuite(EnergyConsumptionSamplesTest.class);

		return suite;
	}

}
