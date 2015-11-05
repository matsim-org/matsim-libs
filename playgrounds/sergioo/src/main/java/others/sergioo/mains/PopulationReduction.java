package others.sergioo.mains;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;

public class PopulationReduction {

	//Main
	/**
	 * 
	 * @param args
	 * 0-Network file
	 * 1-Facilities file
	 * 2-Source population file
	 * 3-Fraction (0,1)
	 * 4-Destination population file
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(args[0]);
		new MatsimFacilitiesReader((MutableScenario) scenario).readFile(args[1]);
		new MatsimPopulationReader(scenario).readFile(args[2]);
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork(), new Double(args[3])).write(args[4]);
	}
	
}
