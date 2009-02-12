package playground.wrashid.DES.util;

import org.matsim.mobsim.jdeqsim.TestEventLog;
import org.matsim.mobsim.jdeqsim.TestMessageFactory;
import org.matsim.mobsim.jdeqsim.TestMessageQueue;
import org.matsim.mobsim.jdeqsim.TestScheduler;
import org.matsim.mobsim.jdeqsim.util.EventLibrary;


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
