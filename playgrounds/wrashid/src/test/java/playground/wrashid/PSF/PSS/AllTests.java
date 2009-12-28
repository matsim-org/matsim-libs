package playground.wrashid.PSF.PSS;



import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for " + AllTests.class.getPackage().getName());

		suite.addTestSuite(FirstPriceSignalMaintainingAlgorithmTests.class);
	

		return suite;
	}

}
           