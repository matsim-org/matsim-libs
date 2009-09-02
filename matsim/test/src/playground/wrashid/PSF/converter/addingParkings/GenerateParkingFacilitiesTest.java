package playground.wrashid.PSF.converter.addingParkings;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;

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
