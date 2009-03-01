package org.matsim.mobsim.jdeqsim;

import org.matsim.mobsim.jdeqsim.util.TestHandlerDetailedEventChecker;
import org.matsim.testcases.MatsimTestCase;


public class TestDESStarter_Berlin extends MatsimTestCase {

	public void test_Berlin_TestHandlerDetailedEventChecker() {
		TestHandlerDetailedEventChecker detailedChecker = new TestHandlerDetailedEventChecker();
		detailedChecker.startTestDES("test/scenarios/berlin/config.xml", false, null, null);
	}

}
