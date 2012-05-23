package playground.christoph.evacuation.analysis;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

public class AgentsInMunicipalityTester {

	public static void main(String[] args) {
		
//		../../matsim/mysimulations/census2000V2/input_1pct/network_ivtch.xml.gz 
//		../../matsim/mysimulations/census2000V2/input_1pct/facilities.xml.gz 
//		../../matsim/mysimulations/census2000V2/input_1pct/plans.xml.gz
//		../../matsim/mysimulations/census2000V2/input_1pct/households.xml.gz
//		../../matsim/mysimulations/census2000V2/input_1pct/householdsObjectAttributes.xml.gz 
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("../../matsim/mysimulations/census2000V2/input_1pct/network.xml.gz");
		
		config.facilities().setInputFile("../../matsim/mysimulations/census2000V2/input_1pct/facilities_person_3060142.xml");
//		config.facilities().setInputFile("../../matsim/mysimulations/census2000V2/input_1pct/facilities_person_3127611.xml");
		
		config.plans().setInputFile("../../matsim/mysimulations/census2000V2/input_1pct/plans_person_3060142.xml");
//		config.plans().setInputFile("../../matsim/mysimulations/census2000V2/input_1pct/plans_person_3127611.xml");
		
		config.households().setInputFile("../../matsim/mysimulations/census2000V2/input_1pct/households_person_3060142.xml");
//		config.households().setInputFile("../../matsim/mysimulations/census2000V2/input_1pct/households_person_3127611.xml");
		config.scenario().setUseHouseholds(true);
	}
}
