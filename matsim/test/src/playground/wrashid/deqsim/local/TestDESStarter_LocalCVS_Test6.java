package playground.wrashid.deqsim.local;

import org.matsim.gbl.Gbl;
import org.matsim.mobsim.deqsim.util.TestHandlerDetailedEventChecker;
import org.matsim.testcases.MatsimTestCase;


public class TestDESStarter_LocalCVS_Test6 extends MatsimTestCase {
	// enable assertion flag allowed
	// contains 67K plans
	public void test_LocalCVS_Test6_TestHandlerDetailedEventChecker() {
		Gbl.reset();

		TestHandlerDetailedEventChecker detailedChecker = new TestHandlerDetailedEventChecker();
		try {
			detailedChecker
					.startTestDES(
							"C:\\data\\SandboxCVS\\ivt\\studies\\wrashid\\test\\test6\\config.xml",
							false, null, null);
		} catch (Exception e) {
			System.out.println("THIS TEST WILL ONLY RUN LOCALLY");
			//e.printStackTrace();
		}
	}

}
