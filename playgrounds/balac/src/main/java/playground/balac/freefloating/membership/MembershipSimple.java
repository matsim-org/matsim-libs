package playground.balac.freefloating.membership;

import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class MembershipSimple {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader populationReader = new PopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(args[0]);
	//	new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		populationReader.readFile(args[1]);
		
		
		

	}

}
