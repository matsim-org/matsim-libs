package playground.mmoyo.algorithms;

import java.util.Random;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.PlanMutateTimeAllocation;
import org.matsim.population.algorithms.TripPlanMutateTimeAllocation;
import org.matsim.pt.PtConstants;

import playground.mmoyo.algorithms.PopulationCleaner;
import playground.mmoyo.utils.DataLoader;

/**Applies time mutation to plans*/
public class PopTimeMutator {
	final Double mutationRange;
	
	public PopTimeMutator(final Double mutationRange){
		this.mutationRange = mutationRange;
	}
	
	public void run (Population population, Random random){
		PlanAlgorithm timeMutator = new PlanMutateTimeAllocation(this.mutationRange, random);
		for(Person person: population.getPersons().values()){
			for (Plan plan : person.getPlans()){
				timeMutator.run(plan);
			}
		}
	}
	
	public void run4transit (Population population){		
		boolean affectingDuration = true ;

		PlanAlgorithm timeMutator =
				new TripPlanMutateTimeAllocation(
						new StageActivityTypesImpl( PtConstants.TRANSIT_ACTIVITY_TYPE ),
						this.mutationRange,
						affectingDuration, MatsimRandom.getLocalInstance());
		for(Person person: population.getPersons().values()){
			for (Plan plan : person.getPlans()){
				timeMutator.run(plan);
			}
		}
	}
	
		
	public static void main(String[] args) {
		String netFilePath;
		String popFilePath;
		Double mutationRange; 
		String output;
			
		if (args.length>0){
			netFilePath = args[0];
			popFilePath = args[1];
			mutationRange = Double.valueOf(args[2]);
			output = args[3];
		}else{
			netFilePath = "../../";
			popFilePath = "../../";
			mutationRange = 7200.;
			output = "";
		}
			
		DataLoader dataLoader = new DataLoader();
		Network net = dataLoader.readNetwork(netFilePath);
		Population pop = dataLoader.readPopulation(popFilePath);
		dataLoader = null;
		new PopulationCleaner().run(pop);
		new PopTimeMutator(mutationRange).run(pop, MatsimRandom.getLocalInstance());
		new PopulationWriter(pop, net).write(output);
	}

}
