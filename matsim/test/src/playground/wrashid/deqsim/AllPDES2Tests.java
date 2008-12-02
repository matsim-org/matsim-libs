package playground.wrashid.deqsim;

import playground.wrashid.PDES2.ZoneMessageQueueTest;
import playground.wrashid.PDES2.util.ConcurrentListMPDSCTest;
import playground.wrashid.PDES2.util.MyPriorityQueueTest;
import playground.wrashid.PHEV.Utility.EnergyConsumptionSamplesTest;
import playground.wrashid.deqsim.TestPDESStarter2_EquilPopulationPlans1Modified1;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllPDES2Tests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for playground.wrashid.deqsim (PDES2)");
		
		// PDES Tests
		suite.addTestSuite(TestPDESStarter2_EquilPopulationPlans1Modified1.class);
		suite.addTestSuite(TestPDESStarter2_equilPlans100.class);
		suite.addTestSuite(TestPDESStarter2_Berlin.class);
		return suite;
	}

	

}
