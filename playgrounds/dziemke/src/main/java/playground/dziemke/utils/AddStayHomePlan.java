package playground.dziemke.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author dziemke
 * Uses a given plans file and adds a stay-home plan to the plans of each person
 * The home location of the newly created stay-home plan is taken from the first plan of the agent
 */
public class AddStayHomePlan {
	// parameters
	static String runId = "run_115";
	static int iteration = 50;
	
	/// input and output files
	static String inputPlansFile = "D:/Workspace/container/demand/output/" + runId + "/ITERS/it." + iteration
			+ "/" + runId + "." + iteration + ".plans.xml.gz";
	static String outputPlansFile = "D:/Workspace/container/demand/output/" + runId + "/ITERS/it." + iteration
			+ "/" + runId + "." + iteration + ".plansStayHome2.xml.gz";
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationReader reader = new PopulationReader(scenario);
		reader.readFile(inputPlansFile);
		Population population = scenario.getPopulation();

		for (Person person : population.getPersons().values()) {			
			Plan plan = person.getPlans().get(0);
			
			Activity homeActivity = (Activity) plan.getPlanElements().get(0);
			
			Plan plan2 = population.getFactory().createPlan();
			plan2.addActivity(homeActivity);			
			
			person.addPlan(plan2);		
		}
		
		// write population file
		//new PopulationWriter(scenario.getPopulation(), null).write(outputBase + "plans.xml.gz");
		new PopulationWriter(scenario.getPopulation(), null).write(outputPlansFile);
		
//		System.out.println("Analysis file " + outputPlansFile + " written.");
	}
}