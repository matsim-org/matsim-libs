package playground.wrashid.lib.tools.plan;

import java.util.LinkedList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;


public class PrintAllActTypesInPlan {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String base="H:/data/experiments/TRBAug2011/runs/ktiRun24/output/";
		String inputPlansFile= base +"output_plans.xml.gz";
		String inputNetworkFile=base+"output_network.xml.gz";
		String inputFacilities=base+"output_facilities.xml.gz";
		
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
