package playground.wrashid.PSF.converter.addingParkings;

import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.lib.GeneralLib;

public class GenerateParkingFacilitiesTest extends MatsimTestCase {

	/*
	 * test, that the number of created facilities corresponds to what is expected.
	 */
	public void testGenerateParkingFacilities(){
		
		String inputPlansFile = "test/input/playground/wrashid/PSF/converter/addParkings/plans2.xml";
		String networkFile = "test/scenarios/berlin/network.xml.gz";
		String outputFacilitiesFile = "output/facilities2.xml";

		GenerateParkingFacilities.generateParkingFacilties(inputPlansFile,networkFile,outputFacilitiesFile);
		
		ActivityFacilitiesImpl facilities = GeneralLib.readActivityFacilities(outputFacilitiesFile);
		
		assertEquals(4, facilities.getFacilities().size());
	}
	
}
