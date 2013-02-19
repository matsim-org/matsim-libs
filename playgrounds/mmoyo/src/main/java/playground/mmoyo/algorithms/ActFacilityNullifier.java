
package playground.mmoyo.algorithms;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.population.algorithms.PersonAlgorithm;
import playground.mmoyo.utils.DataLoader;

public class ActFacilityNullifier implements PersonAlgorithm {

	@Override
	public void run(Person person){
		for(Plan plan: person.getPlans()){
			for (PlanElement pe: plan.getPlanElements()){
				if (pe instanceof Activity) {
					((ActivityImpl)pe).setFacilityId(null);
				}
			}
		}
	}
	public void run(Population population){
		for (Person person : population.getPersons().values()){
			this.run(person);
		}
	}
	
	public static void main(String[] args) {
		String popFilePath;
		String netFilePath;
		String outputFilePath;
		
		if (args.length==3){
			popFilePath = args[0];
			netFilePath = args[1];
			outputFilePath = args[2];
		}else{
			popFilePath = "../../";
			netFilePath = "../../";
			outputFilePath = "../";
		}

		DataLoader dLoader = new DataLoader();
		Population population = dLoader.readPopulation(popFilePath);
		new ActFacilityNullifier().run(population);
		Network net = dLoader.readNetwork(netFilePath);
		System.out.println("writing output plan file..." + outputFilePath);
		new PopulationWriter(population, net).write(outputFilePath);
	}

}

