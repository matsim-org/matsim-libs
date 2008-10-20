package playground.wrashid.PDES2;

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
		TestSuite suite = new TestSuite("Tests for playground.wrashid.util");

		suite.addTestSuite(ZoneMessageQueueTest.class);
		
		return suite;
	}

	

}
