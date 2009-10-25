package playground.wrashid.PSF;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for " + AllTests.class.getPackage().getName());

		suite.addTest(playground.wrashid.PSF.converter.addingParkings.AllTests.suite());
		suite.addTest(playground.wrashid.PSF.data.AllTests.suite());
		suite.addTest(playground.wrashid.PSF.PSS.AllTests.suite());
		suite.addTest(playground.wrashid.PSF.singleAgent.AllTests.suite());
		
		return suite;
	}
}
