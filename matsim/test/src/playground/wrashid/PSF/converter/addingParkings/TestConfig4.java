package playground.wrashid.PSF.converter.addingParkings;

import org.matsim.testcases.MatsimTestCase;

public class TestConfig4  extends MatsimTestCase {

	public void testConfig(){
		String configFilePath="test/input/playground/wrashid/PSF/converter/addParkings/config4.xml";
		new GeneralTestOptimizedCharger(configFilePath).optimizedChargerTest();
	}
	 
}
 