package playground.wrashid.PSF.converter.addingParkings;



import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for " + AllTests.class.getPackage().getName());

		suite.addTestSuite(AddParkingsToPlansTest.class);
		suite.addTestSuite(GenerateParkingFacilitiesTest.class);
		suite.addTestSuite(TestConfig3.class);
		suite.addTestSuite(TestConfig4.class);

		return suite;
	}

}
           