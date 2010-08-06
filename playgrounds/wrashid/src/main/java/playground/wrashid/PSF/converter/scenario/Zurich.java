package playground.wrashid.PSF.converter.scenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;

import playground.wrashid.PSF.converter.addingParkings.AddParkingsToPlans;
import playground.wrashid.PSF.converter.addingParkings.GenerateParkingFacilities;
import playground.wrashid.lib.GeneralLib;

public class Zurich {
	public static void main(String[] args) {
		String inputNetworkPath="a:/data/matsim/input/runRW1003/network-osm-ch.xml.gz";
		String inputPlansPath = "a:/data/matsim/input/runRW1003/plans-1pml-miv-dilzh30km-unmapped.xml.gz";
		String inputFacilitiesPath="a:/data/matsim/input/runRW1003/facilities.zrhCutC.xml.gz";
		
		ScenarioImpl scenario = (ScenarioImpl) GeneralLib.readScenario(inputPlansPath, inputNetworkPath, inputFacilitiesPath);

		// generate parking facilities
		GenerateParkingFacilities.generateParkingFacilties(scenario);		
		
		// generate plans with parking
		AddParkingsToPlans.generatePlanWithParkingActs(scenario);
		
		
	
	}
}
