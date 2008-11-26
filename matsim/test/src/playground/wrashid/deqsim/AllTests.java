package playground.wrashid.deqsim;

import playground.wrashid.PDES2.ZoneMessageQueueTest;
import playground.wrashid.PDES2.util.ConcurrentListMPDSCTest;
import playground.wrashid.PDES2.util.MyPriorityQueueTest;
import playground.wrashid.PHEV.Utility.EnergyConsumptionSamplesTest;
import playground.wrashid.deqsim.TestPDESStarter2_EquilPopulationPlans1Modified1;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for playground.wrashid.deqsim");

		// DES Tests
		suite.addTestSuite(TestDESStarter_EquilPopulationPlans1Modified1.class);
		suite.addTestSuite(TestDESStarter_equilPlans100.class);
		suite.addTestSuite(TestDESStarter_Berlin.class);
		
		// PDES Tests
		suite.addTestSuite(TestPDESStarter2_EquilPopulationPlans1Modified1.class);
		suite.addTestSuite(TestPDESStarter2_equilPlans100.class);
		suite.addTestSuite(TestPDESStarter2_Berlin.class);
		return suite;
	}

	

}
