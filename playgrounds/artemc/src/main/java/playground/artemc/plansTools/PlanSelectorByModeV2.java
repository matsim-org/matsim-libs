package playground.artemc.plansTools;

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
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class PlanSelectorByModeV2 {

	private static final Logger log = Logger.getLogger(PlanSelectorByMode.class);

	public static void main(String[] args) {
		
		String inputPopulationFile = args[0];
		String outputPopulationFileCar = args[1];
		String outputPopulationFilePT = args[2];

		/*Create scenario and load population*/
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		log.info("Reading population...");
		new MatsimPopulationReader(scenario).readFile(inputPopulationFile);
		new MatsimPopulationReader(scenario2).readFile(inputPopulationFile);
		Population populationPT = ((MutableScenario)scenario).getPopulation();
		Population populationCar = ((MutableScenario)scenario2).getPopulation();
		
		
		ArrayList<Id> carUsersForRemoval = new ArrayList<Id>();
		ArrayList<Id> ptUsersForRemoval = new ArrayList<Id>();
		System.out.println("Number of persons: "+populationPT.getPersons().size());
		for(Id personId:populationPT.getPersons().keySet()){
			
			String parts[] = personId.toString().split("_");
			String hh = parts[0];
						
			Plan plan = populationPT.getPersons().get(personId).getSelectedPlan();
			boolean ptPerson = false;
			boolean carPerson = false;

			/*Check if person used Pt in the last selected plan*/
			for(PlanElement planElement:plan.getPlanElements()){
				if(planElement instanceof Leg){
					Leg leg = (Leg) planElement;
					if(leg.getMode().equals("pt") || leg.getMode().equals("transit_walk")){
						ptPerson = true;
					}
					if(leg.getMode().equals("car")){
						carPerson = true;
					}
				}
			}

			/*If person didn't use PT, add it to removal List, otherwise clear all plans and keep only selected PT Plan*/
			if(ptPerson)
			{
				populationPT.getPersons().get(personId).getPlans().clear();
				populationPT.getPersons().get(personId).addPlan(plan);
				ptUsersForRemoval.add(personId);
			}
			else{
				if(carPerson){
					populationCar.getPersons().get(personId).getPlans().clear();
					populationCar.getPersons().get(personId).addPlan(plan);
				}
				else{
					ptUsersForRemoval.add(personId);
				}
				carUsersForRemoval.add(personId);
			}
		}

		/*Remove all persons without PT*/
		for(Id personId:carUsersForRemoval){
			populationPT.getPersons().remove(personId);
		}
		
		/*Remove all persons without Car*/
		for(Id personId:ptUsersForRemoval){
			populationCar.getPersons().remove(personId);
		}


		/*Write out new population file*/
		System.out.println("New number of persons in PT population: "+populationPT.getPersons().size());
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(outputPopulationFilePT);
		System.out.println("New number of persons in Car population: "+populationCar.getPersons().size());
		new PopulationWriter(scenario2.getPopulation(), scenario.getNetwork()).write(outputPopulationFileCar);
	}

}