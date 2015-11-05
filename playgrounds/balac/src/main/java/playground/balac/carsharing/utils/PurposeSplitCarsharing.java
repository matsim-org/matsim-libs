package playground.balac.carsharing.utils;

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
import org.matsim.facilities.FacilitiesReaderMatsimV1;

public class PurposeSplitCarsharing {


	public void run(String plansFilePath, String networkFilePath, String facilitiesfilePath, String outputFilePath) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(networkFilePath);
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		populationReader.readFile(plansFilePath);
		int countE = 0;
		int countW = 0;
		int countS = 0;
		int countL = 0;
		int count = 0;
		boolean cs = false;
		int work = 0;
		int number = 0;
		for (Person p: scenario.getPopulation().getPersons().values()) {
			ArrayList<Activity> c = new ArrayList<Activity>();
			Activity a = null;
			for (PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				
				if (pe instanceof Activity) {
					a = (Activity) pe;
					
				}
				else if (pe instanceof Leg) {
					
					if (a.getType().startsWith("work")) {
						
						if (((Leg) pe).getMode().equals( "carsharingwalk") )
							number++;
						//startWork = true;
					}
					
				}
								
				if (pe instanceof Leg) {
					if ( (((Leg) pe).getMode().equals("twowaycarsharing"))) {
						work++;
						
					}
					if ( (((Leg) pe).getMode().equals("twowaycarsharing"))) {
						cs = true;
						
					}
					else 
						cs = false;
					
				}
				else if (pe instanceof Activity) {
					if (cs) {
						
						if (((Activity) pe).getType().startsWith("work")){
							
							countW++;
							
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
					}
					
					
				}
			}

			
		}
		System.out.println(number);
		System.out.println(work);
		System.out.println(countS);
		System.out.println(countW);
		System.out.println(countE);
		System.out.println(countL);
		System.out.println(count);
		System.out.println((double)countS/count);
		
		//new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFileV4(outputFilePath + "/plans_1p.xml");		
		
	}
	
	
	
	public static void main(String[] args) {
		PurposeSplitCarsharing cp = new PurposeSplitCarsharing();
		
		String plansFilePath = args[0]; 
		String networkFilePath = args[1];
		String facilitiesfilePath = args[2];
		String outputFolder = args[3];
		
		cp.run(plansFilePath, networkFilePath,facilitiesfilePath, outputFolder);

	}

}
