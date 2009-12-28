import junit.framework.Test;
import junit.framework.TestSuite;


public class AllTests {
	public static Test suite() {
		TestSuite suite = new TestSuite("All tests for MATSim");
		//$JUnit-BEGIN$

		// run unit tests
		suite.addTest(org.matsim.AllTests.suite());
		
		//$JUnit-END$
		return suite ;
	}
}
