package playground.mmoyo.algorithms;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

import playground.mmoyo.utils.DataLoader;

/**set minimal value score*/
public class PlanScoreMinValue extends AbstractPersonAlgorithm {

	@Override
	public void run(Person person) {
		for (Plan plan: person.getPlans()){
			plan.setScore(Double.NEGATIVE_INFINITY);
		}
	}
	
	public static void main(String[] args) {
		String populationFile = "../../";
		String networkFile = "../../";
		String outputFile = "../../";
		
		Scenario scenario = new DataLoader().readNetwork_Population(networkFile, populationFile );
	
		new PlanScoreNullifier().run(scenario.getPopulation());
		System.out.println("writing output plan file..." + outputFile);
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(outputFile);
		System.out.println("done");
	}

}
