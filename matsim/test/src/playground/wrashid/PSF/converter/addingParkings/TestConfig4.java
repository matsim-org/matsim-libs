package playground.wrashid.PSF.converter.addingParkings;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestCase;

public class TestConfig4  extends MatsimTestCase {

	public void testConfig(){
		String basePathOfTestData="test/input/playground/wrashid/PSF/converter/addParkings/";
		String networkFile = "test/scenarios/berlin/network.xml.gz";
		
		// generate parking facilities
		GenerateParkingFacilities.generateParkingFacilties(basePathOfTestData + "plans2.xml", networkFile, "output/facilities4.xml");		
		
		// generate plans with parking
		AddParkingsToPlans.generatePlanWithParkingActs(basePathOfTestData + "plans2.xml", networkFile, "output/plans4.xml", "output/facilities4.xml");
		
		// start simulation run
		String configFilePath="test/input/playground/wrashid/PSF/converter/addParkings/config4.xml";
		Config config = loadConfig(configFilePath);
		Controler controler = new Controler(config);
		controler.setCreateGraphs(false);
		new GeneralTestOptimizedCharger(controler).optimizedChargerTest();
	} 
	  
}
 