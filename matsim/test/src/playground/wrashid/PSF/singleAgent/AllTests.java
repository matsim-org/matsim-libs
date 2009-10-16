package playground.wrashid.PSF.singleAgent;



import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for " + AllTests.class.getPackage().getName());

		suite.addTestSuite(BasicTests.class);
		suite.addTestSuite(AdvancedTests.class);
		suite.addTestSuite(ScoringTests.class);
		

		return suite;
	}

}
           