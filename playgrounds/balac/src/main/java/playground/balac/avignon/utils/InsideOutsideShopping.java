package playground.balac.avignon.utils;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class InsideOutsideShopping {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		
			populationReader.readFile(args[0]);
			networkReader.readFile(args[1]);
			int numberInside = 0;
			int numberOutside = 0;
			
			double coordX = 683217.0;
			double coordY = 247300.0;
			
			for(Person p: scenario.getPopulation().getPersons().values()) {
				
				for(PlanElement pe: p.getSelectedPlan().getPlanElements()) {
					
					if (pe instanceof Activity) {
						
						if (((Activity) pe).getType().equals( "shopgrocery" )) {
							
							if (Math.sqrt(Math.pow(((Activity) pe).getCoord().getX() - coordX, 2) +(Math.pow(((Activity) pe).getCoord().getY() - coordY, 2))) > 5000) {
								
								numberOutside++;
							}
							else 
								numberInside++;
							
							
						}
					}
					
					
				}
			}	
			System.out.println(numberInside);
			System.out.println(numberOutside);


	}

}
