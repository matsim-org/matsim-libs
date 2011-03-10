package playground.mmoyo.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.apache.log4j.Logger;

public class PopulationFilter2 {
	private final static Logger log = Logger.getLogger(PopulationFilter2.class);
	
	/** returns from the population only the given persons id's**/	
	private void run (Population population, List<Id> rowList){
		//get by exclusion the list of persons to be deleted 
		List<Id> delebPersonList = new ArrayList<Id>();
		for (Person person : population.getPersons().values()){
			if (!rowList.contains(person.getId())){
				delebPersonList.add(person.getId());
			}
		}
		
		//delete persons who are not in the list
		for (Id delebPersonId : delebPersonList){
			population.getPersons().remove(delebPersonId);
		}
	
		delebPersonList = null;
	}

	/**reads id's from a text file*/
	public List<Id> readAgentFromTxtFile(final String idsFilePath  ){
		//reads idFilePaths
		List<Id> idList = new ArrayList<Id>();
		try {
		    BufferedReader in = new BufferedReader(new FileReader(idsFilePath));
		    String str_row;
		    while ((str_row = in.readLine()) != null) {
		    	idList.add(new IdImpl(str_row));
		    }
		    in.close();
		} catch (IOException e) {
		}
		
		return idList;
	}
	
	public List<Id> getRandomAgents(final Population pop, final int agentNum){
		List<Id> idList = new ArrayList<Id>();
	
		Random random = new Random();
		String str_selected = "Selected person: ";
		int popSize = pop.getPersons().size();
		
		Id[]idArray = pop.getPersons().keySet().toArray(new Id[popSize]);
		
		for (int i=0; i<agentNum; i++){
			 int rand_i = random.nextInt(popSize);
			 Id randId = idArray[rand_i];
			 idList.add(randId);
			 log.info(str_selected + randId.toString());
		 }

		random = null;
		str_selected= null;
		idArray = null;
		return idList;
	}
	
	public static void main(String[] args) {
		List<Id> persIds;
		String popFile;
		String netFile;
		String txtFile;
		String outFile; 
		
		if (args.length==4){
			popFile = args[0];
			netFile = args[1];
			txtFile = args[2];
			outFile = args[3];
		}else{
			popFile = "../playgrounds/mmoyo/output/tmp/w6.0d0.0t1200.0_w10.0d0.0t240.0_w8.0d0.5t720.0.xml.gz";//"../playgrounds/mmoyo/output/tmp/w6.0d0.0t1200.0_w10.0d0.0t240.0_w8.0d0.5t720.0.xml.gz";//"I:/alltest5/output/routedPlan_walk6.0_dist0.0_tran1200.0.xml.gz";
			netFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
			txtFile = "../playgrounds/mmoyo/output/cadyts/persAvoid_812550.1.txt";
			outFile = "../playgrounds/mmoyo/output/cadyts/agentsSample.xml.gz";
		}
		
		DataLoader dataLoader = new DataLoader();
		Population pop = dataLoader.readPopulation(popFile);
		PopulationFilter2 popFileter2 = new PopulationFilter2(); 
		
		//get random persons
		//persIds = popFileter2.getRandomAgents(pop, 4);
		
		//get persons from text File
		persIds = popFileter2.readAgentFromTxtFile(txtFile);

		//get persons directly with code
		/*
		persIds = new ArrayList<Id>()
		persIds.add(new IdImpl("11100482X1"));
		persIds.add(new IdImpl(""));
		persIds.add(new IdImpl(""));
		*/

		popFileter2.run(pop, persIds);
		NetworkImpl net = dataLoader.readNetwork(netFile);		
		new PopulationWriter(pop, net).write(outFile);

		
	}

}
