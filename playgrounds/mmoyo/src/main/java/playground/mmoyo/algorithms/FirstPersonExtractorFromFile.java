package playground.mmoyo.algorithms;

import java.io.File;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PersonAlgorithm;

import playground.mmoyo.io.PopSecReader;
import playground.mmoyo.utils.DataLoader;

public class FirstPersonExtractorFromFile implements PersonAlgorithm{
	int personNum;
	int personCount=0;
	Network net;
	Population newPop;
	final String outDir;

	public FirstPersonExtractorFromFile (Network net, final int personNum, final String outDir){
		this.net = net;
		this.personNum = personNum;
		this.outDir = outDir;
		this.newPop = new DataLoader().createScenario().getPopulation();
	}
	
	@Override
	public void run(Person person) {
		newPop.addPerson(person);
		if(++personCount == personNum){
			//write the persons and stop the execution
			PopulationWriter popWriter = new PopulationWriter(this.newPop, this.net );
			popWriter.write(this.outDir + "/firstPersons.xml" );
			System.exit(0);	
		}
	}
	
	public static void main(String[] args) {
		String populationFile = "../../input/juli/routedPlan_walk10.0_dist0.0_tran240.0.xml.gz";
		String networkFile = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		int personNum = 100;
		
		DataLoader dataLoader = new DataLoader();
		ScenarioImpl scn = (ScenarioImpl) dataLoader.createScenario();
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scn);
		matsimNetReader.readFile(networkFile);
		
		FirstPersonExtractorFromFile firstPersonExtractorFromFile = new FirstPersonExtractorFromFile(scn.getNetwork(), personNum, new File(populationFile).getParent()); 
		PopSecReader popSecReader = new PopSecReader (scn, firstPersonExtractorFromFile);
		popSecReader.readFile(populationFile);
	}

}
