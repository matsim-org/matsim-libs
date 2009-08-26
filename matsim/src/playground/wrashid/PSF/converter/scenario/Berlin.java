package playground.wrashid.PSF.converter.scenario;

import playground.wrashid.PSF.converter.addingParkings.AddParkingsToPlans;
import playground.wrashid.PSF.converter.addingParkings.GeneralTestOptimizedCharger;
import playground.wrashid.PSF.converter.addingParkings.GenerateParkingFacilities;

public class Berlin {

	/*
	 * generate parking facilities for berlin 1% scenario and also generate the updated plans
	 */
	public static void main(String[] args) {
		String basePathOfData="test/scenarios/berlin/";
		String networkFile = basePathOfData+  "network.xml.gz";
		
		// generate parking facilities
		GenerateParkingFacilities.generateParkingFacilties(basePathOfData + "plans_hwh_1pct.xml.gz", networkFile, "output/facilities.xml");		
		
		// generate plans with parking
		AddParkingsToPlans.generatePlanWithParkingActs(basePathOfData + "plans_hwh_1pct.xml.gz", networkFile, "output/plans.xml", "output/facilities.xml");
		
		// start simulation run
		//String configFilePath="test/input/playground/wrashid/PSF/converter/addParkings/config4.xml";
		//new GeneralTestOptimizedCharger(configFilePath).optimizedChargerTest();
	}
	
}
