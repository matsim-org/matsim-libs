package playground.dziemke.utils;

import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author dziemke
 * 
 * Reads in a plans file and copies person with their plans into a new plans file according to a
 * configurable {@values electionProbability}. Then writes new plans file to a given location.
 */
public class PlanFileMinimizer {
	// Parameters
	static double selectionProbability = 0.01;
	static boolean onlySelectedPlan = true;
	//static String runId = "run_138";
	//static int iteration = 250;
	
	
	// Input and output files
	static String inputPlansFile = "D:/Workspace/container/demand/input/cemdap2matsim/24/plans.xml.gz";
	//static String inputPlansFile = "D:/Workspace/container/demand/output/" + runId + "/ITERS/it." + iteration
	//		+ "/" + runId + "." + iteration + ".plans.xml.gz";
	static String outputPlansFile = "D:/Workspace/container/demand/input/cemdap2matsim/24/plansSelection10.xml.gz";
	//static String outputPlansFile = "D:/Workspace/container/demand/output/" + runId + "/ITERS/it." + iteration
	//		+ "/" + runId + "." + iteration + ".plansSmall.xml.gz";
	
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationReaderMatsimV5 reader = new PopulationReaderMatsimV5(scenario);
		reader.readFile(inputPlansFile);
		Population population = scenario.getPopulation();
		
		
		Config config2 = ConfigUtils.createConfig();
		Scenario scenario2 = ScenarioUtils.createScenario(config2);
		Population population2 = scenario2.getPopulation();
		
		for (Person person : population.getPersons().values()) {
			
			Random random = new Random();
			double randomNumber = random.nextDouble();
			if (randomNumber < selectionProbability) {
				if (onlySelectedPlan == true) {
					Plan selectedPlan = person.getSelectedPlan();
					Id id = person.getId();
					Person person2 = population.getFactory().createPerson(id);
					person2.addPlan(selectedPlan);
					population2.addPerson(person2);
				} else {
					population2.addPerson(person);
				}
			} else {
				// do nothing
			}
		}
		
				
		
		// write population file
		new PopulationWriter(scenario2.getPopulation(), null).write(outputPlansFile);
		
		System.out.println("Minimized plans file " + outputPlansFile + " written.");
	}
}