package playground.mmoyo.algorithms;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.PersonImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import playground.mmoyo.utils.DataLoader;


public class SetFirstPlanSelected extends AbstractPersonAlgorithm {
	//PlanScoreNullifier planScoreNullifier = new PlanScoreNullifier();
	
	@Override
	public void run(Person person) {
		PersonImpl p2 = (PersonImpl) person;
		p2.setSelectedPlan(p2.getPlans().get(0));
		//planScoreNullifier.run(p2);	
	}
	
	public static void main(String[] args) {
		String populationFile;
		String networkFile;
		String outputFile;
		
		if (args.length>0){
			populationFile= args[0];
			networkFile= args[1];
			outputFile= args[2];
		}else{
			populationFile = "../../runs_manuel/CalibLineM44/automCalib10xTimeMutated/10xrun/it.500/500.plans.xml.gz";
			networkFile = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
			outputFile = "../../mmoyo/output/tmp/10x_onlyM44users_firstplanselected.xml.gz";
		}
		
		Scenario scenario = new DataLoader().readNetwork_Population(networkFile, populationFile );
		new SetFirstPlanSelected().run(scenario.getPopulation());
		System.out.println("writing output plan file..." + outputFile);
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(outputFile);
		System.out.println("done");
	}

}
