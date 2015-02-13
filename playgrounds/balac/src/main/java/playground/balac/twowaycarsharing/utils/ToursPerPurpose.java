package playground.balac.twowaycarsharing.utils;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

public class ToursPerPurpose {
	public void run(String plansFilePath, String networkFilePath, String facilitiesfilePath) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(networkFilePath);
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		populationReader.readFile(plansFilePath);
		
		int count = 0;
		int countWork = 0;
		int countLeisure = 0;
		int countEducation = 0;
		int countShop = 0;
		boolean carsh = false;
		for (Person p: scenario.getPopulation().getPersons().values()) {
			
			for (PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Leg) {
					
					if ((((Leg) pe).getMode().equals("carsharing")) || (((Leg) pe).getMode().equals( "carsharingwalk"))) {
						carsh = true;
					}
					else
						carsh = false;
				}
				else if (pe instanceof Activity && carsh == true) {
					if (!((Activity) pe).getType().startsWith("carsharing") && !((Activity) pe).getType().startsWith("home")) count++;
					if (((Activity) pe).getType().startsWith("work")) countWork++;
					if (((Activity) pe).getType().startsWith("shop")) countShop++;
					if (((Activity) pe).getType().startsWith("education")) countEducation++;
					if (((Activity) pe).getType().startsWith("leisure")) countLeisure++;
					
				}
			}

			
		}
		System.out.println(countWork);
		System.out.println(countShop);
		System.out.println(countEducation);
		System.out.println(countLeisure);
		System.out.println(count);
		
		
		//new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFileV4(outputFilePath + "/plans_1p.xml");		
		
	}
	
	public static void main(String[] args) {
		
		ToursPerPurpose cp = new ToursPerPurpose();
				
		String plansFilePath = args[0]; 
		String networkFilePath = args[1];
		String facilitiesfilePath = args[2];
		
		
		cp.run(plansFilePath, networkFilePath,facilitiesfilePath);
	}
}
