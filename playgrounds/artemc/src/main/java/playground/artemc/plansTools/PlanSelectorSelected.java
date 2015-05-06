package playground.artemc.plansTools;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class PlanSelectorSelected {

	private static final Logger log = Logger.getLogger(PlanSelectorByMode.class);


	public static void main(String[] args) {

		String inputPopulationFile = args[0];
		String outputPopulationFile = args[1];

		/*Create scenario and load population*/
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		log.info("Reading population...");
		new MatsimPopulationReader(scenario).readFile(inputPopulationFile);
		Population population = scenario.getPopulation();

		System.out.println("Number of persons: "+population.getPersons().size());

		for(Id personId:population.getPersons().keySet()){

			Plan plan = population.getPersons().get(personId).getSelectedPlan();
			population.getPersons().get(personId).getPlans().clear();
			population.getPersons().get(personId).addPlan(plan);

		}

		PlanRouteStripper stripper = new PlanRouteStripper();
		stripper.run(scenario.getPopulation());

		/*Write out new population file*/
		System.out.println("New number of persons: "+population.getPersons().size());
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(outputPopulationFile);
	}

}
