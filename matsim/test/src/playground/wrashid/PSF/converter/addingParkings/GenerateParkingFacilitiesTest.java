package playground.wrashid.PSF.converter.addingParkings;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;

public class GenerateParkingFacilitiesTest extends MatsimTestCase {

	/*
	 * test, that the number of created facilities corresponds to what is expected.
	 */
	public void testGenerateParkingFacilities(){
		
		String inputPlansFile = "test/input/playground/wrashid/PSF/converter/addParkings/plans2.xml";
		String networkFile = "test/scenarios/berlin/network.xml.gz";
		String outputFacilitiesFile = "output/facilities2.xml";

		GenerateParkingFacilities.generateParkingFacilties(inputPlansFile,networkFile,outputFacilitiesFile);
		
		ActivityFacilities facilities = new ActivityFacilitiesImpl();
		
		try {
			new MatsimFacilitiesReader(facilities).parse(outputFacilitiesFile);
			
			assertEquals(4, facilities.getFacilities().size());
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
