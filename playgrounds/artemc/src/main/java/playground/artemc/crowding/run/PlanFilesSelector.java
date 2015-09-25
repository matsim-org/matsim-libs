package playground.artemc.crowding.run;

import java.util.ArrayList;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class PlanFilesSelector {

	private static final Logger log = Logger.getLogger(PlanFilesSelector.class);
	private static String inputPopulationFile = "C:/Workspace/roadpricingSingapore/output_SiouxFalls/CorridorOutput_20000PT_shortDay_500it_walk/output_plans.xml.gz";
	private static String outputPopulationFile = "C:/Workspace/roadpricingSingapore/output_SiouxFalls/CorridorOutput_20000PT_shortDay_500it_walk/onyPtPlans.xml";


	public static void main(String[] args) {
		
		/*Create scenario and load population*/
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		log.info("Reading population...");
		new MatsimPopulationReader(scenario).readFile(inputPopulationFile);
		Population population = ((ScenarioImpl)scenario).getPopulation();
		
		ArrayList<Id> personsForRemoval = new ArrayList<Id>();
		System.out.println("Number of persons: "+population.getPersons().size());
		for(Id personId:population.getPersons().keySet()){
			Plan plan = population.getPersons().get(personId).getSelectedPlan();
			boolean ptPerson = false;

			/*Check if person used Pt in the last selected plan*/
			for(PlanElement planElement:plan.getPlanElements()){
				if(planElement instanceof Leg){
					Leg leg = (Leg) planElement;
					if(leg.getMode().equals("pt") || leg.getMode().equals("transit_walk")){
						ptPerson = true;
					}
				}
			}

			/*If person didn't use PT, add it to removal List, otherwise clear all plans and keep only selected PT Plan*/
			if(ptPerson)
			{
				population.getPersons().get(personId).getPlans().clear();
				population.getPersons().get(personId).addPlan(plan);
			}
			else{
				personsForRemoval.add(personId);
			}
		}

		/*Remove all persons without PT*/
		for(Id personId:personsForRemoval){
			population.getPersons().remove(personId);
		}

		/*Write out nre populaiton file*/
		System.out.println("New number of persons: "+population.getPersons().size());
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(outputPopulationFile);
	}

}
