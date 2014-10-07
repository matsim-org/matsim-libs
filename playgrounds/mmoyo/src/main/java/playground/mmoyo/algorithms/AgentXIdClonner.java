package playground.mmoyo.algorithms;

import java.io.File;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;

import playground.mmoyo.io.TXT_IdReader;
import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.FirstPersonsExtractor;

/**Clones only some agents given in a list in the same population*/
public class AgentXIdClonner{
	final private static String SEP = "_";
	
	public void run (Population pop, List<Id<Person>> persIdList, int numClones){
		PersonClonner clonner = new PersonClonner();
		for (int i=0; i<=numClones; i++){
			for (Id<Person> id :persIdList){
				Person person = pop.getPersons().get(id);
				for (int j=0; j<=numClones; j++){
					Id<Person> newId = Id.create(person.getId().toString() + SEP + j, Person.class); 
					Person newPerson = clonner.run(person, newId);
					pop.addPerson(newPerson);
				}				
			}	
		}
	}
	
	public static void main(String[] args) {
		String popFilePath = "../../";
		String idsFilePath = "../../";
		String netFilePath = "../../";
		int numClones = 4;
		
		DataLoader dataLoader = new DataLoader();
		Population pop = dataLoader.readPopulation(popFilePath);
		List<Id<Person>> persIdList = new TXT_IdReader().readAgentFromTxtFile(idsFilePath);
		new AgentXIdClonner().run(pop, persIdList, numClones);
		
		//write population in same Directory
		String outputFile = new File(popFilePath).getParent() + File.separatorChar + "/someclones/clonnedPlans.xml.gz";		
		Network net = new DataLoader().readNetwork(netFilePath);
		PopulationWriter popWriter= new PopulationWriter(pop, net);
		popWriter.write(outputFile);	
	
		//write sample in first pop directory
		popWriter = new PopulationWriter(new FirstPersonsExtractor().run(pop, 2), net);
		File file = new File(popFilePath);
		popWriter.write(file.getParent() + "/someclones/planSample.xml") ;
		System.out.println("done");
	}



}
