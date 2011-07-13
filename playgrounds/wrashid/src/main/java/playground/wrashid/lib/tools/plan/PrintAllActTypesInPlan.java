package playground.wrashid.lib.tools.plan;

import java.util.LinkedList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.IntegerValueHashMap;

public class PrintAllActTypesInPlan {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inputPlansFile="K:/Projekte/herbie/output/demandCreation/plans.xml.gz";
		String inputNetworkFile="K:/Projekte/matsim/data/switzerland/networks/ivtch-multimodal/zh/network.multimodal-wu.xml.gz";
		String inputFacilities="K:/Projekte/herbie/output/demandCreation/facilitiesWFreight.xml.gz";
		
		Scenario scenario= GeneralLib.readScenario(inputPlansFile, inputNetworkFile, inputFacilities);
		
		printAllActTypes(scenario.getPopulation());
	}

	
	/**
	 * print summary of all act types used in the plans.
	 * @param population
	 */
	public static void printAllActTypes(Population population){
		IntegerValueHashMap<String> actTypes=new IntegerValueHashMap<String>();
		for (Person person:population.getPersons().values()){
			for (Plan plan:person.getPlans()){
				for (PlanElement pe:plan.getPlanElements()){
					if (pe instanceof Activity){
						Activity activity=(Activity) pe;
						actTypes.increment(activity.getType());
					}
				}
			}
		}
		
		for (String activityType:actTypes.getKeySet()){
			System.out.println(activityType + " => " + actTypes.get(activityType));
		}
	}
	
}
