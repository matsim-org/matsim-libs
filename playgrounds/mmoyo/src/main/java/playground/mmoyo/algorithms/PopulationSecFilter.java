package playground.mmoyo.algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

import playground.mmoyo.io.PopSecReader;
import playground.mmoyo.utils.DataLoader;

/** Creates a population only with agent id's specified in a list. Uses sequence reader*/
public class PopulationSecFilter extends AbstractPersonAlgorithm {
	private List<Id<Person>> strIdList = new ArrayList<>();
	private Population newPop;

    {
        ScenarioImpl sc = (ScenarioImpl) new DataLoader().createScenario();
        newPop = PopulationUtils.createPopulation(sc.getConfig(), sc.getNetwork());
    }

    @Override
	public void run(Person person) {
		if (strIdList.contains(person.getId())){
			newPop.addPerson(person);
		}
	}

	public List<Id<Person>> getStrIdList() {
		return strIdList;
	}
	
	public Population getNewPop() {
		return newPop;
	}
	
	public static void main(String[] args) {
		String netFilePath = "../../";
		String popFilePath = "../../";
	
		DataLoader dataLoader = new DataLoader();
		ScenarioImpl scn = (ScenarioImpl) dataLoader.createScenario();
		PopulationSecFilter populationSecFilter = new PopulationSecFilter(); 
		populationSecFilter.strIdList.add(Id.create("11140292", Person.class));
		
		PopSecReader popSecReader = new PopSecReader (scn, populationSecFilter);
		popSecReader.readFile(popFilePath);
		
		Network net = dataLoader.readNetwork(netFilePath);
		File file = new File(popFilePath);
		PopulationWriter popWriter = new PopulationWriter(populationSecFilter.newPop, net);
		popWriter.write(file.getParent() + "/" + file.getName() + "SecFilteredPlan.xml.gz");
	}
	
}