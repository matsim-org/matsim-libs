package playground.wrashid.DES.util;

import org.matsim.mobsim.deqsim.TestEventLog;
import org.matsim.mobsim.deqsim.TestMessageFactory;
import org.matsim.mobsim.deqsim.TestMessageQueue;
import org.matsim.mobsim.deqsim.TestScheduler;
import org.matsim.mobsim.deqsim.util.EventLibrary;


import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for playground.wrashid.DES.utils");

		suite.addTestSuite(TestEventLibrary.class);
		suite.addTestSuite(TestFastQueue.class);
		
		return suite;
	}

	

}
