package playground.wrashid.PHEV.Utility;


import playground.wrashid.PHEV.Utility.EnergyConsumptionSamplesTest;
import playground.wrashid.deqsim.TestPDESStarter2_EquilPopulationPlans1Modified1;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for playground.wrashid.PHV.Utility");

		suite.addTestSuite(EnergyConsumptionSamplesTest.class);
		
		return suite;
	}

	

}
