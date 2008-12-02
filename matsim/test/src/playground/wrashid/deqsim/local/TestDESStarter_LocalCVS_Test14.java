package playground.wrashid.deqsim.local;

import org.matsim.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.util.TestHandlerDetailedEventChecker;

public class TestDESStarter_LocalCVS_Test14 extends MatsimTestCase {
	// enable assertion flag allowed
	// contains 161K plans
	public void test_LocalCVS_Test14_TestHandlerDetailedEventChecker() {
		Gbl.reset();

		TestHandlerDetailedEventChecker detailedChecker = new TestHandlerDetailedEventChecker();
		try {
			detailedChecker
					.startTestDES(
							"C:\\data\\SandboxCVS\\ivt\\studies\\wrashid\\test\\test14\\config.xml",
							false, null, null);
		} catch (Exception e) {
			System.out.println("THIS TEST WILL ONLY RUN LOCALLY");
			e.printStackTrace();
		}
	}

}
