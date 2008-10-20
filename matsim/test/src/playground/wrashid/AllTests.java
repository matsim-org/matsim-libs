package playground.wrashid;

import playground.wrashid.PDES.util.ConcurrentListMPDSCTest;
import playground.wrashid.PDES.util.MyPriorityQueueTest;
import playground.wrashid.PDES2.ZoneMessageQueueTest;
import playground.wrashid.PHEV.Utility.EnergyConsumptionSamplesTest;
import playground.wrashid.deqsim.DEQSimStarterTest;
import playground.wrashid.deqsim.PDESStarter2Test;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for playground.wrashid");
		suite.addTest(playground.wrashid.deqsim.AllTests.suite());
		suite.addTest(playground.wrashid.PDES.util.AllTests.suite());
		suite.addTest(playground.wrashid.PDES2.AllTests.suite());
		suite.addTest(playground.wrashid.PHEV.Utility.AllTests.suite());
		
		return suite;
	}
}
