package playground.wrashid.customTestSuites;

import playground.wrashid.PDES2.ZoneMessageQueueTest;
import playground.wrashid.PDES2.util.ConcurrentListMPDSCTest;
import playground.wrashid.PDES2.util.MyPriorityQueueTest;
import playground.wrashid.PHEV.Utility.EnergyConsumptionSamplesTest;
import playground.wrashid.deqsim.TestPDESStarter2_EquilPopulationPlans1Modified1;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllDESTests {
// should run with -ea flag enabled
	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for playground.wrashid.DES");
		suite.addTest(playground.wrashid.DES.AllTests.suite());
		suite.addTest(playground.wrashid.deqsim.AllDESTests.suite());
		
		
		return suite;
	}

	

}
