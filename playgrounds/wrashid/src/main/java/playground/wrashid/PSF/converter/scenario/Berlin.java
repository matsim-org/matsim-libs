package playground.wrashid.PSF.converter.scenario;

import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.wrashid.PSF.converter.addingParkings.AddParkingsToPlans;
import playground.wrashid.PSF.converter.addingParkings.GenerateParkingFacilities;


public class Berlin {

	/*
	 * generate parking facilities for berlin 1% scenario and also generate the updated plans
	 */
	public static void main(String[] args) {
		String basePathOfData="test/scenarios/berlin/";
		String networkFile = basePathOfData+  "network.xml.gz";
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFile);
		new MatsimPopulationReader(scenario).readFile(basePathOfData + "plans_hwh_1pct.xml.gz");

		// generate parking facilities
		GenerateParkingFacilities.generateParkingFacilties(scenario);		
		
		// generate plans with parking
		AddParkingsToPlans.generatePlanWithParkingActs(scenario);
		
		// start simulation run
		//String configFilePath="test/input/playground/wrashid/PSF/converter/addParkings/config4.xml";
		//Controler controler = new Controler(scenario);
		//new GeneralTestOptimizedCharger(controler).optimizedChargerTest();
	}
	
}
