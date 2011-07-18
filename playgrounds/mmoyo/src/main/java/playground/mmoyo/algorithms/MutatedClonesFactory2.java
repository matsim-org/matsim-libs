package playground.mmoyo.algorithms;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.PlanMutateTimeAllocation;

import playground.mmoyo.utils.DataLoader;

/**create clones for original persons and more clones of existing clones*/
public class MutatedClonesFactory2 {
	
	public void run(Population pop, final int numClones){
		PersonClonner personClonner = new PersonClonner(); 
		final String SEP = "_";
		final String crit = "X";		
		ClonDetector clonDetector = new ClonDetector(crit);

		//create mutators with diff seeds 
		PlanAlgorithm[] mutatorArray = new PlanAlgorithm[20];
		for (int i=0; i<20;i++){
			mutatorArray[i]= new PlanMutateTimeAllocation(7200,MatsimRandom.getLocalInstance());
			((PlanMutateTimeAllocation)mutatorArray[i]).setUseActivityDurations(true);
		}
		
		List<Person> newPersonList = new ArrayList<Person>();
		
		for (Person person: pop.getPersons().values()){
			for (int i=0; i<numClones;i++){
				Id newId = new IdImpl(person.getId().toString() + SEP + (i+1));
				Person newPerson = personClonner.run(person, newId);
	
				//mutate
				int cIndex = clonDetector.getClonIndex(person.getId().toString()) ;
				if (cIndex == -1){ //it is NOT a clon
					cIndex=0;
				}
				for (Plan plan : person.getPlans()){
					mutatorArray[cIndex].run(plan);
				}
				for (Plan plan : newPerson.getPlans()){
					mutatorArray[cIndex+10].run(plan);
				}

				newPersonList.add(newPerson);
			}
		}
		
		//add new persons to original population
		for (Person newPerson: newPersonList){
			pop.addPerson(newPerson);
		}
	}
	
	public static void main(String[] args) {
		String popFilePath = "../../input/juni/passengersTracked.xml.gz";  
		String netFile = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_network.xml.gz";
		
		DataLoader dLoader = new DataLoader();
		Population pop = dLoader.readPopulation(popFilePath);
			
		int numClones = 5;
		new MutatedClonesFactory2().run(pop, numClones);

		Network net = dLoader.readNetwork(netFile);
		PopulationWriter popwriter = new PopulationWriter(pop, net );
		popwriter.write(new File(popFilePath).getParent() + "/clonedMutatedPersons.xml.gz") ;
		
	}

}
