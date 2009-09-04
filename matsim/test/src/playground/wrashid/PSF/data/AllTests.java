package playground.wrashid.PSF.data;



import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for " + AllTests.class.getPackage().getName());

		suite.addTestSuite(HubLinkMappingTest.class);
		suite.addTestSuite(HubPriceInfoTest.class);

		return suite;
	}

}
           