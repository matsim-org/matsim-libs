package playground.balac.freefloating.membership;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class MembershipSimple {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[0]);
	//	new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		populationReader.readFile(args[1]);
		
		
		

	}

}
