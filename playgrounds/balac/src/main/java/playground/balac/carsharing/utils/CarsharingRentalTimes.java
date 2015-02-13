package playground.balac.carsharing.utils;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

public class CarsharingRentalTimes {
	public void run(String plansFilePath, String networkFilePath, String facilitiesfilePath, String outputFilePath) {
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(networkFilePath);
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		populationReader.readFile(plansFilePath);
		
		int count = 0;
		int[] rentalTimes = new int[24];
		boolean cs = false;
		int count1 = 0;
		double time = 0.0;
		int members = 0;
		for (Person p: scenario.getPopulation().getPersons().values()) {
			PersonImpl per = (PersonImpl)p;
			if (per.getTravelcards() !=null && per.getTravelcards().contains("ch-HT-mobility"))
				members++;
			if (per.getId().toString().equals("6551679"))
				System.out.println();
			time = 0.0;
			double startTime = 0.0;
			double endTime = 0.0;
			cs = false;
			Activity previousActivity = null;
			Leg previousLeg = null;
			for (PlanElement pe:p.getSelectedPlan().getPlanElements()) {
					
				if (pe instanceof Activity) {
					previousActivity = (Activity) pe;
					
				}
				else if (pe instanceof Leg) {
						if (!cs) { 
						
							if (((Leg) pe).getMode().equals( "carsharingwalk" )) {	
								cs = true;
								if (previousActivity.getEndTime() > 0)
									startTime = previousActivity.getEndTime() + ((Leg) pe).getTravelTime();
							}
							
						}
						else {
							int index = p.getSelectedPlan().getPlanElements().indexOf(pe);
							
							if (((Leg) pe).getMode().equals( "carsharingwalk" )) {
								endTime = previousLeg.getDepartureTime() + previousLeg.getTravelTime();
								cs = false;
								
									if (endTime - startTime > 0 && endTime - startTime < 86400) {
										if (endTime - startTime < 1800) count1++;
										rentalTimes[(int)((endTime - startTime) / 3600)]++;
										count++;
									}
									else {
										
									}
							
							}
							
						}	
						previousLeg = (Leg) pe;
				}	
				
			}			
		}
		System.out.println(members);
		System.out.println(count1);
		for (int i = 0; i < rentalTimes.length; i++) 
			System.out.println((double)rentalTimes[i]/(double)count * 100.0);
			
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
