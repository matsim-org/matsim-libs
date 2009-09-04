package playground.wrashid;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for " + AllTests.class.getPackage().getName());

		suite.addTest(playground.wrashid.lib.AllTests.suite());
		suite.addTest(playground.wrashid.PHEV.Utility.AllTests.suite());
		suite.addTest(playground.wrashid.tryouts.performance.AllTests.suite());
		suite.addTest(playground.wrashid.PSF.AllTests.suite());
		
		return suite;
	}
}
