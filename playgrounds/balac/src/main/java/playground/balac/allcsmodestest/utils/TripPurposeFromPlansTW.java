package playground.balac.allcsmodestest.utils;

import java.util.ArrayList;

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


public class TripPurposeFromPlansTW {

	public void run(String plansFilePath, String networkFilePath) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(networkFilePath);
		populationReader.readFile(plansFilePath);
		int countE = 0;
		int countW = 0;
		int countS = 0;
		int countL = 0;
		int countH = 0;
		int count = 0;
		int work = 0;
		boolean cs = false;
		int number = 0;
		int[] distanceCar = new int[200];
		int countCar =0;
		double d = 0.0;
		int cc = 0;
		
		for (Person p: scenario.getPopulation().getPersons().values()) {
			ArrayList<Activity> c = new ArrayList<Activity>();
			Activity a = null;
			cs = false;
			for (PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				
				if (pe instanceof Activity) {
					a = (Activity) pe;
					
				}
				else if (pe instanceof Leg) {
					if (((Leg) pe).getMode().equals("car")) {
						d+=((Leg) pe).getRoute().getDistance();
						cc++;
						if (((Leg) pe).getRoute().getDistance() < 200000) {
							distanceCar[(int)(((Leg) pe).getRoute().getDistance() / 1000)]++;
							countCar++;
						}
					}
					
					if (a.getType().startsWith("home")) {
						
						if (((Leg) pe).getMode().equals("walk_rb"))
							number++;
						//startWork = true;
					}
					
				}
								
				if (pe instanceof Leg) {
					
					if ( (((Leg) pe).getMode().equals("twowaycarsharing")) || ((Leg) pe).getMode().equals("walk_rb")) {
						cs = true;
						
					}
					else 
						cs = false;
					
				}
				else if (pe instanceof Activity) {
					if (cs) {
						
						if (((Activity) pe).getType().startsWith("work")){
							if (!c.isEmpty())
								work++;
							else {
							
							}
							countW++;
							//time += (((Activity) pe).getMaximumDuration());
							c.add((Activity)pe);
							
							count++; 
						}
						else if (((Activity) pe).getType().startsWith("education")){
							countE++;
						count++;}
						else if (((Activity) pe).getType().startsWith("shop")){
							countS++;
						count++;}
						else if (((Activity) pe).getType().startsWith("leisure")) {
							countL++;
						count++;}
					/*	else if (((Activity) pe).getType().startsWith("home")) {
							countH++;
						count++;}*/
					}
					
					
				}
			}

			
		}
		//System.out.println(work);
		System.out.println("Starting from home trips: " + number);
		System.out.println("Shopping trips: " + (double)countS/count*100.0);
		System.out.println("Work trips: " + (double)countW/count*100.0);
		System.out.println("Education trips: " + (double)countE/count*100.0);
		System.out.println("Leisure trips: " + (double)countL/count*100.0);
		System.out.println("Home trips: " + (double)countH/count*100.0);
		System.out.println("Total trips: " + count);
		for (int i = 0; i < distanceCar.length; i++) 
			System.out.println((double)distanceCar[i]/(double)countCar * 100.0);
		System.out.println(d/1000.0/cc);				
		System.out.println(cc);

		
		
	}
	
	
	
	public static void main(String[] args) {
		TripPurposeFromPlansTW tripPurposeFromPlansTW = new TripPurposeFromPlansTW();
				
		tripPurposeFromPlansTW.run(args[0], args[1]);

	}

}
