package playground.balac.carsharing.utils;

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

public class CarsharingDepartureTimes {
	public void run(String plansFilePath, String networkFilePath, String facilitiesfilePath, String outputFilePath) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(networkFilePath);
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		populationReader.readFile(plansFilePath);
		
		int count = 0;
		int[] bla = new int[30];
		
		for (Person p: scenario.getPopulation().getPersons().values()) {			
			
			Activity a = null;
			for (PlanElement pe:p.getSelectedPlan().getPlanElements()) {
					
				if (pe instanceof Activity) 
					a = (Activity) pe;				
				
				else if (pe instanceof Leg) {
						
						if (((Leg) pe).getMode().equals( "carsharingwalk" )) 		
							if (!a.getType().equals( "carsharingInteraction" )) {
								
								
								
								bla[(int)((a.getEndTime() + ((Leg)pe).getTravelTime()) / 3600)]++;
								count++;
							}
				}
			}
		}
		
		for (int i = 0; i < bla.length; i++) 
			System.out.println((double)bla[i]/(double)count);
			
		}
	
	public static void main(String[] args) {
		CarsharingDepartureTimes cp = new CarsharingDepartureTimes();
		
		String plansFilePath = args[0]; 
		String networkFilePath = args[1];
		String facilitiesfilePath = args[2];
		String outputFolder = args[3];
		
		cp.run(plansFilePath, networkFilePath,facilitiesfilePath, outputFolder);

	}

}
