package playground.wrashid.tryouts.performance;

import org.matsim.core.mobsim.jdeqsim.TestEventLog;
import org.matsim.core.mobsim.jdeqsim.TestMessageFactory;
import org.matsim.core.mobsim.jdeqsim.TestMessageQueue;
import org.matsim.core.mobsim.jdeqsim.TestScheduler;
import org.matsim.core.mobsim.jdeqsim.util.EventLibrary;

import playground.wrashid.DES.util.TestEventLibrary;


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
