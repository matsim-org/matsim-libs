package playground.balac.twowaycarsharing.utils;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

public class CarsharingRentalTimes {
	public void run(String plansFilePath, String networkFilePath, String facilitiesfilePath, String outputFilePath) {
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(networkFilePath);
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		populationReader.readFile(plansFilePath);
		
		int count = 0;
		int[] rentalTimes = new int[24];
		boolean cs = false;
		
		double time = 0.0;
		for (Person p: scenario.getPopulation().getPersons().values()) {
			
			
			time = 0.0;
			
			cs = false;
			for (PlanElement pe:p.getSelectedPlan().getPlanElements()) {
					
				if (pe instanceof Activity) {
				
					if (cs) {
					
						if (!((Activity) pe).getType().equals( "carsharinginteraction" ))
							time += ((Activity) pe).getMaximumDuration();
					}
				}
				else if (pe instanceof Leg) {
				
						if (!cs) { 
						
							if (((Leg) pe).getMode().equals( "carsharingwalk" ))	
								cs = true;	
						}
						else {
						
							if (((Leg) pe).getMode().equals( "carsharingwalk" )) {
							
								cs = false;
								if ( time > 0.0) {
								
									rentalTimes[(int)time/3600]++;
									count++;
									time = 0.0;
								}
							
							}
							else if (((Leg) pe).getMode().equals( "carsharing" ))
								time += ((Leg) pe).getTravelTime();
						}				
				}			
			}			
		}
		
		for (int i = 0; i < rentalTimes.length; i++) 
			System.out.println((double)rentalTimes[i]/(double)count);
			
		}
	
	public static void main(String[] args) {
		CarsharingRentalTimes cp = new CarsharingRentalTimes();
		
		String plansFilePath = args[0]; 
		String networkFilePath = args[1];
		String facilitiesfilePath = args[2];
		String outputFolder = args[3];
		
		cp.run(plansFilePath, networkFilePath,facilitiesfilePath, outputFolder);

	}

}
