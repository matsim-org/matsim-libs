package playground.wrashid.PSF.converter.addingParkings;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.testcases.MatsimTestCase;

public class TestConfig4 extends MatsimTestCase {

	public void testConfig(){
		String basePathOfTestData=getPackageInputDirectory();
		String networkFile = "test/scenarios/berlin/network.xml.gz";

		String configFilePath=basePathOfTestData + "config4.xml";
		Config config = loadConfig(configFilePath);
		
		ScenarioImpl scenario = new ScenarioImpl(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		new MatsimPopulationReader(scenario).readFile(basePathOfTestData + "plans2.xml");
		
		// generate parking facilities
		GenerateParkingFacilities.generateParkingFacilties(scenario);		
		// generate plans with parking
		AddParkingsToPlans.generatePlanWithParkingActs(scenario);
		
		// start simulation run
		Controler controler = new Controler(scenario);
		controler.setCreateGraphs(false);
		new GeneralTestOptimizedCharger(controler).optimizedChargerTest();
	}
	  
}
 