package playground.mzilske.modechoice;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;

public class ModeChoice {
	
	public static void main(String[] args) {
		
		Scenario scenario = new ScenarioImpl();
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile("../../detailedEval/net/network.xml");
		
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
		PopulationImpl populationImpl = (PopulationImpl) scenario.getPopulation();
		
		populationImpl.setIsStreaming(true);
		populationImpl.addAlgorithm(new ApplyToSelectedPlan(new ChangeLegModeOfOneSubtour()));
		PopulationWriter algo = new PopulationWriter(populationImpl, scenario.getNetwork());
		populationImpl.addAlgorithm(algo);
		
		algo.startStreaming("output/wurst.xml");
		reader.readFile("../../detailedEval/pop/befragte-personen/routed-plans.xml");
		algo.closeStreaming();
		
		
	}

}
