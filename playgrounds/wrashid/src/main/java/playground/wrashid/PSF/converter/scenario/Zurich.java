package playground.wrashid.PSF.converter.scenario;

import org.matsim.core.scenario.ScenarioImpl;

import playground.wrashid.PSF.converter.addingParkings.AddParkingsToPlans;
import playground.wrashid.PSF.converter.addingParkings.GenerateParkingFacilities;
import playground.wrashid.lib.GeneralLib;

public class Zurich {
	public static void main(String[] args) {
		String inputNetworkPath="a:/data/matsim/input/runRW1003/network-osm-ch.xml.gz";
		String inputPlansPath = "a:/data/matsim/input/runRW1003/plans-1pml-miv-dilzh30km-osm-mapped.xml.gz";
		String inputFacilitiesPath="a:/data/matsim/input/runRW1003/facilities.zrhCutC.xml.gz";
		
		String outputPlansPath="a:/data/matsim/input/runRW1003/plans-1pml-miv-dilzh30km-osm-mapped-withParkArrAndDepActs.xml.gz";
		String outputFacilitiesPath="a:/data/matsim/input/runRW1003/facilities.zrhCutC-withParkingArrAndDep.xml.gz";
		
		ScenarioImpl scenario = (ScenarioImpl) GeneralLib.readScenario(inputPlansPath, inputNetworkPath, inputFacilitiesPath);

		// generate parking facilities
		GenerateParkingFacilities.generateParkingFacilties(scenario);		
		
		// generate plans with parking
		AddParkingsToPlans.generatePlanWithParkingActs(scenario);
		
		// write output files
		GeneralLib.writePopulation(scenario.getPopulation(), scenario.getNetwork(), outputPlansPath);
		GeneralLib.writeActivityFacilities(scenario.getActivityFacilities(), outputFacilitiesPath);
	}
	
}
