package playground.mmoyo.algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.population.algorithms.PlansFilterByLegMode;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;

import playground.mmoyo.utils.DataLoader;

/**reads all clones and return their original agents if they exist */
public class OriginalPersonFinder {

	public void run (Population pop){
		Map <String, List<Id>> personMap = new TreeMap <String, List<Id>>();
		final String SUF = "X";
		ClonDetector clonDetector = new ClonDetector(SUF);
		
		for (Id id : pop.getPersons().keySet()){
			String strId = id.toString();
			int i = clonDetector.getClonIndex(strId);
			
			String root = (i>-1)?  strId.substring(0, strId.indexOf(SUF)): strId;
			
			if(!personMap.keySet().contains(root)){
				List<Id> personsList = new ArrayList<Id>();
				
				//add  original person 
				if (root.equals(strId)){
					personsList.add(id);
				}
				
				//add 4 clons if they are available
				int numClones=0;
				int ii=9;
				while(numClones<4 && ii>0){
					Id possibleId = new IdImpl(root + SUF + ii);
					if (pop.getPersons().keySet().contains(possibleId)){
						numClones++;
						personsList.add(possibleId);
					}
					ii--;
				}
				personMap.put(root, personsList);
			}
		}

				
		//get persons
		List<Id> retainIds = new ArrayList<Id>();
		for(Map.Entry <String, List<Id>> entry: personMap.entrySet() ){
			String key = entry.getKey(); 
			List<Id> list = entry.getValue();
			System.out.println("key : " + key);
			for (Id id : list){
				System.out.println("            " + id);
				retainIds.add(id);
			}
		}
	
		pop.getPersons().keySet().retainAll(retainIds);
		System.out.println(retainIds.size());
	}

	public static void main(String[] args) {
		final String netFilePath; 
		final String popFilePath;
		
		if (args.length>0){
			popFilePath = args[0];
			netFilePath = args[1];
		}else{
			popFilePath = "../../input/popx5_8agents.xml.gz";
			netFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		}
		
		DataLoader dataLoader = new DataLoader();
		Population pop = dataLoader.readPopulation(popFilePath);
		
		//remove plans with car legs
		PlansFilterByLegMode plansFilter = new PlansFilterByLegMode( TransportMode.car, PlansFilterByLegMode.FilterType.removeAllPlansWithMode) ;
		plansFilter.run(pop);
		
		new OriginalPersonFinder().run(pop);

		Network net = dataLoader.readNetwork(netFilePath);
		PopulationWriter popwriter = new PopulationWriter(pop, net );
		popwriter.write(new File(popFilePath).getParent() + "/selectedPersons.xml.gz") ;
		System.out.println("done");
	}		


}
