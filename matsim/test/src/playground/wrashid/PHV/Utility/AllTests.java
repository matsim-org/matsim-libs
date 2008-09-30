package playground.wrashid.PHV.Utility;

import playground.wrashid.PDES.util.ConcurrentListMPDSCTest;
import playground.wrashid.PDES.util.MyPriorityQueueTest;
import playground.wrashid.PDES2.ZoneMessageQueueTest;
import playground.wrashid.PHV.Utility.EnergyConsumptionSamplesTest;
import playground.wrashid.deqsim.DEQSimStarterTest;
import playground.wrashid.deqsim.PDESStarter2Test;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for playground.wrashid.PHV.Utility");

		suite.addTestSuite(EnergyConsumptionSamplesTest.class);
		
		return suite;
	}

	

}
