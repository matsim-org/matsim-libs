package org.matsim.mobsim.deqsim;

import org.matsim.gbl.Gbl;
import org.matsim.mobsim.deqsim.util.TestHandlerDetailedEventChecker;
import org.matsim.testcases.MatsimTestCase;


public class TestDESStarter_Berlin extends MatsimTestCase {

	public void test_Berlin_TestHandlerDetailedEventChecker() {
		Gbl.reset();

		TestHandlerDetailedEventChecker detailedChecker = new TestHandlerDetailedEventChecker();
		detailedChecker.startTestDES("test/scenarios/berlin/config.xml", false, null, null);
	}

}
