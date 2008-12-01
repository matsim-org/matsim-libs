package playground.wrashid.DES;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for playground.wrashid.DES");

		suite.addTestSuite(TestEventLog.class);
		suite.addTestSuite(TestMessageFactory.class);
		suite.addTestSuite(TestMessageQueue.class);
		suite.addTestSuite(TestScheduler.class);
		
		return suite;
	}

	

}
