package playground.wrashid;

import playground.wrashid.PDES2.ZoneMessageQueueTest;
import playground.wrashid.PDES2.util.ConcurrentListMPDSCTest;
import playground.wrashid.PDES2.util.MyPriorityQueueTest;
import playground.wrashid.PHEV.Utility.EnergyConsumptionSamplesTest;
import playground.wrashid.deqsim.TestPDESStarter2_EquilPopulationPlans1Modified1;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for playground.wrashid");
		suite.addTest(playground.wrashid.deqsim.AllTests.suite());
		suite.addTest(playground.wrashid.PDES2.util.AllTests.suite());
		suite.addTest(playground.wrashid.PDES2.AllTests.suite());
		suite.addTest(playground.wrashid.PHEV.Utility.AllTests.suite());
		
		return suite;
	}
}
