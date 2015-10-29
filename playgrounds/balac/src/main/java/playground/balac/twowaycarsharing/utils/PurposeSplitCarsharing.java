package playground.balac.twowaycarsharing.utils;

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

public class PurposeSplitCarsharing {


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
		for (Person p: scenario.getPopulation().getPersons().values()) {
			ArrayList<Activity> c = new ArrayList<Activity>();
			Activity a = null;
			cs = false;
			for (PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				
				if (pe instanceof Activity) {
					a = (Activity) pe;
					
				}
				else if (pe instanceof Leg) {
					
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
						else if (((Activity) pe).getType().startsWith("home")) {
							countH++;
						count++;}
					}
					
					
				}
			}

			
		}
		System.out.println(work);
		System.out.println(number);
		System.out.println((double)countS/count*100.0);
		System.out.println((double)countW/count*100.0);
		System.out.println((double)countE/count*100.0);
		System.out.println((double)countL/count*100.0);
		System.out.println((double)countH/count*100.0);
		System.out.println(count);
		System.out.println((double)countS/count);
		
		//new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFileV4(outputFilePath + "/plans_1p.xml");		
		
	}
	
	
	
	public static void main(String[] args) {
		PurposeSplitCarsharing cp = new PurposeSplitCarsharing();
		
		String plansFilePath = args[0]; 
		String networkFilePath = args[1];
		
		
		cp.run(plansFilePath, networkFilePath);

	}

}
