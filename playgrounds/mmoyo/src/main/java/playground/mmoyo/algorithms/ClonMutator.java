package playground.mmoyo.algorithms;

import java.io.File;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.PlanMutateTimeAllocation;

import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.FirstPersonsExtractor;

/** Mutates existing clones with suffix X in population file*/
public class ClonMutator {
	//private static final Logger log = Logger.getLogger(ClonMutator2.class);
	int max =9; //maxNum of found clones //->find it automatically
	final String x= "X";
	
	public void mutateClons (Population pop, int mutRange){
		ClonDetector clonDetector = new ClonDetector(x);
		List<Id<Person>> clons = clonDetector.run(pop);
		
		//create mutators with diff seeds
		PlanAlgorithm[] mutatorArray = new PlanAlgorithm[max];
		for (int i=0; i<max;i++){
			mutatorArray[i]= new PlanMutateTimeAllocation(mutRange,MatsimRandom.getLocalInstance());
//			((PlanMutateTimeAllocation)mutatorArray[i]).setUseActivityDurations(true);
		}
		
		//mutate clones according to diff mutation seed
		for (Id<Person> clonId : clons){
			Person clon = pop.getPersons().get(clonId);
			int clonIndex= clonDetector.getClonIndex(clon.getId().toString());
			for (Plan plan : clon.getPlans()){  
				//log.info(clonId +  " " + clonIndex + " " + mutatorArray.length + " " + (plan ==null));
				mutatorArray[clonIndex-1].run(plan);
			}
		}
	}
	
	public static void main(String[] args) {
		String popFile = "../../";
		String netFilePath= "../../";
		String outFile = "../../";
		DataLoader dataLoader = new DataLoader();
		Population pop = dataLoader.readPopulation(popFile);
		new ClonMutator().mutateClons(pop, 7200);
	
		//write
		Network net = dataLoader.readNetwork(netFilePath);
		PopulationWriter popWriter = new PopulationWriter(pop, net);
		popWriter.write(outFile);
		
		//write a sample
		popWriter = new PopulationWriter(new FirstPersonsExtractor().run(pop,4), net);
		File file = new File (outFile);
		popWriter.write(file.getParent() + "/mutSamplePlans.xml") ;
	}
}
