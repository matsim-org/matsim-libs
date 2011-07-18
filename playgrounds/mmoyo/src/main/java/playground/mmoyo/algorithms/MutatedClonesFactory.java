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
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.PlanMutateTimeAllocation;

import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.FirstPersonsExtractor;

/** Creates clones from a population and mutates their time with different seeds*/
public class MutatedClonesFactory extends AbstractPersonAlgorithm{
	private final String SEP = "_";
	private int cloneNum;
	List<Person> newPersonsList = new ArrayList<Person>();
	PlanAlgorithm[]mutatorArray ;
	
	public MutatedClonesFactory (int cloneNum, int mutRange){
		this.cloneNum = cloneNum;
		
		//create mutators with diff seeds
		PlanAlgorithm[] mutatorArray = new PlanAlgorithm[cloneNum];
		for (int i=0; i<cloneNum;i++){
			mutatorArray[i]= new PlanMutateTimeAllocation(mutRange,MatsimRandom.getLocalInstance());
			((PlanMutateTimeAllocation)mutatorArray[i]).setUseActivityDurations(true);
		}	
	}
	
	@Override
	public void run(Person person){
		for (int i=0; i<cloneNum;i++){
			//clone
			Id newId = new IdImpl(person.getId().toString() + SEP + (i+2));
			Person newPerson = new PersonClonner().run(person, newId);
				
			//mutate cloned plan
			for (Plan newPlan :newPerson.getPlans()){
				mutatorArray[i].run(newPlan);
			}
		
			//add cloned person to newPersonsList
			newPersonsList.add(newPerson);
		}
	}
	
	
	
	public static void main(String[] args) {
		int mutRange;
		int numMutClons;
		String popFilePath;
		String netFilePath;
		
		if (args.length>0){
			mutRange = Integer.valueOf(args[0]);
			numMutClons = Integer.valueOf(args[1]);
			popFilePath = args[2];
			netFilePath = args[3];
		}else{
			mutRange = 7200;
			numMutClons = 10;
			popFilePath = "../../input/juni/passengersTracked.xml.gz";
			netFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		}
		DataLoader dataLoader = new DataLoader();
		Population pop = dataLoader.readPopulation(popFilePath);
		new MutatedClonesFactory(numMutClons, mutRange).run(pop);
		
		//
		
		//write with a sample in first pop directory
		Network net = dataLoader.readNetwork(netFilePath);
		File file = new File(popFilePath);
		PopulationWriter popWriter = new PopulationWriter(pop, net);
		popWriter.write(file.getParent() + "/clonMutatedPlan.xml.gz") ;
		
		int sampleSize = 10;
		pop = new FirstPersonsExtractor().run(pop, (numMutClons+1)*sampleSize);
		popWriter = new PopulationWriter(pop, net);
		popWriter.write(file.getParent() + "/clonMutatedPlanSample.xml") ;
		System.out.println("done");
		
	}
}
