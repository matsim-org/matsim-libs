package playground.mzilske.modechoice;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class ModeChoice {
	
	public static void main(String[] args) {
		
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile("../../detailedEval/net/network.xml");
		
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
		PopulationImpl populationImpl = (PopulationImpl) scenario.getPopulation();
		
		populationImpl.setIsStreaming(true);
		populationImpl.addAlgorithm(new ApplyToSelectedPlan(new SubtourModeChoice(new Config())));
		PopulationWriter algo = new PopulationWriter(populationImpl, scenario.getNetwork());
		populationImpl.addAlgorithm(algo);
		
		algo.startStreaming("output/wurst.xml");
		reader.readFile("../../detailedEval/pop/befragte-personen/routed-plans.xml");
		algo.closeStreaming();
		
		
	}

}
