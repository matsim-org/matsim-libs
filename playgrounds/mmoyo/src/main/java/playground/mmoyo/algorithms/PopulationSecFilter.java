package playground.mmoyo.algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

import playground.mmoyo.io.PopSecReader;
import playground.mmoyo.utils.DataLoader;

/** Creates a population only with agent id's specified in a list. Uses sequence reader*/
public class PopulationSecFilter extends AbstractPersonAlgorithm {
	private List<Id> strIdList = new ArrayList<Id>();
	private Population newPop = new PopulationImpl((ScenarioImpl) new DataLoader().createScenario()); 
	
	@Override
	public void run(Person person) {
		if (strIdList.contains(person.getId())){
			newPop.addPerson(person);
		}
	}

	public static void main(String[] args) {
		String netFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		String popFilePath = "../../input/newDemand/bvg.run190.25pct.100.plans.selected.cleanedXYlinks.xml.gzNoCarPlans.xml.gzplansWtPtLegs.xml.gz";
	
		DataLoader dataLoader = new DataLoader();
		ScenarioImpl scn = (ScenarioImpl) dataLoader.createScenario();
		PopulationSecFilter populationSecFilter = new PopulationSecFilter(); 
		populationSecFilter.strIdList.add(new IdImpl("b1050213"));
		
		PopSecReader popSecReader = new PopSecReader (scn, populationSecFilter);
		popSecReader.readFile(popFilePath);
		
		Network net = dataLoader.readNetwork(netFilePath);
		File file = new File(popFilePath);
		PopulationWriter popWriter = new PopulationWriter(populationSecFilter.newPop, net);
		popWriter.write(file.getParent() + "/" + file.getName() + "SecFilteredPlan.xml.gz");
	}	
}