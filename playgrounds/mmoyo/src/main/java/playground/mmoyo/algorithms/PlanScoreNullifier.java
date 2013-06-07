package playground.mmoyo.algorithms;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.PersonImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.FirstPersonsExtractor;

public class PlanScoreNullifier extends AbstractPersonAlgorithm {

	@Override
	public void run(Person person) {
		for (Plan plan: person.getPlans()){
			plan.setScore(null);
		}
	}
	
	public static void main(String[] args) {
		String populationFile;
		String networkFile;
		String outputFile;
		
		if (args.length>0){
			populationFile= args[0];
			networkFile= args[1];
		}else{
			populationFile = "../../";
			networkFile = "../../";
		}
		
		Scenario scenario = new DataLoader().readNetwork_Population(networkFile, populationFile );
		
		new PlanScoreNullifier().run(scenario.getPopulation());
		File file = new File(populationFile);
		outputFile = file.getPath() + "reduced.mxl.gz";
		System.out.println("writing output plan file..." + outputFile);
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(outputFile);
		System.out.println("done");
		
		//write sample 10 persons
		Population popSample = new FirstPersonsExtractor().run(scenario.getPopulation(), 10);
		
		System.out.println("writing output plan file...");
		PopulationWriter popwriter = new PopulationWriter(popSample, scenario.getNetwork());
		popwriter.write(outputFile + ".planSample.xml") ;
		System.out.println("done");
		
	}

}
