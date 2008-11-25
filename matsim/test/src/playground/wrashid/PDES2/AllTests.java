package playground.wrashid.PDES2;

import playground.wrashid.PDES2.ZoneMessageQueueTest;
import playground.wrashid.PDES2.util.ConcurrentListMPDSCTest;
import playground.wrashid.PDES2.util.MyPriorityQueueTest;
import playground.wrashid.PHEV.Utility.EnergyConsumptionSamplesTest;
import playground.wrashid.deqsim.DEQSimStarterTest;
import playground.wrashid.deqsim.TestPDESStarter2_EquilPopulationPlans1Modified1;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for playground.wrashid.util");

		suite.addTestSuite(ZoneMessageQueueTest.class);
		
		return suite;
	}

	

}
