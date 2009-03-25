package playground.wrashid.DES;

import org.matsim.core.mobsim.jdeqsim.TestEventLog;
import org.matsim.core.mobsim.jdeqsim.TestMessageFactory;
import org.matsim.core.mobsim.jdeqsim.TestMessageQueue;
import org.matsim.core.mobsim.jdeqsim.TestScheduler;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for playground.wrashid.DES");

		suite.addTest(playground.wrashid.DES.util.AllTests.suite());
		
		return suite;
	}

	

}
