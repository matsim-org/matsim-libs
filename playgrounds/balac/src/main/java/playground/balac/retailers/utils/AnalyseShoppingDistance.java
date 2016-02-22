package playground.balac.retailers.utils;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

public class AnalyseShoppingDistance {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(args[0]);
		new FacilitiesReaderMatsimV1(scenario).readFile(args[1]);
		populationReader.readFile(args[2]);
		double distance = 0.0;
		int[] bins = new int[20];
		int number = 0;
		for (Person p: scenario.getPopulation().getPersons().values()) {
			int counter = 0;
			
			for(PlanElement pe: p.getSelectedPlan().getPlanElements()) {
				
				if (pe instanceof Activity) {
					
					if (((Activity) pe).getType().equals("shopgrocery")) {
						
						if (((Activity)p.getSelectedPlan().getPlanElements().get(counter - 2)).getType().startsWith("home") ||
								((Activity)p.getSelectedPlan().getPlanElements().get(counter - 2)).getType().startsWith("work") ||
								((Activity)p.getSelectedPlan().getPlanElements().get(counter - 2)).getType().startsWith("education")) {
							
							distance = CoordUtils.calcEuclideanDistance(((Activity) pe).getCoord(), ((Activity)p.getSelectedPlan().getPlanElements().get(counter - 2)).getCoord());
							if (distance < 10000) {
								bins[(int)distance/500]++;
								number++;
							}
							
							
							
						}
						else if (((Activity)p.getSelectedPlan().getPlanElements().get(counter + 2)).getType().startsWith("home") ||
								((Activity)p.getSelectedPlan().getPlanElements().get(counter + 2)).getType().startsWith("work") ||
								((Activity)p.getSelectedPlan().getPlanElements().get(counter + 2)).getType().startsWith("education")) {
							
							
							distance = CoordUtils.calcEuclideanDistance(((Activity) pe).getCoord(), ((Activity)p.getSelectedPlan().getPlanElements().get(counter + 2)).getCoord());
							if (distance < 10000) {
								bins[(int)distance/500]++;
								number++;
							}
							
						}
					
					}
				}
				counter++;	
			}
		}
		System.out.println(distance / (double)number);
		System.out.println(number);
		for(int i = 0 ; i <bins.length; i++){
			System.out.println(bins[i]);
		}

	}

}
