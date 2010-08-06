package playground.wrashid.lib.tools.plan;

import java.util.LinkedList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

import playground.wrashid.lib.GeneralLib;

public class PrintAllActTypesInPlan {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inputPlansFile="V:/data/cvs/ivt/studies/switzerland/plans/ivtch/census2000v2_dilZh30km_10pct/plans.xml.gz";
		String inputNetworkFile="V:/data/cvs/ivt/studies/switzerland/networks/ivtch/network.xml";
		String inputFacilities="V:/data/cvs/ivt/studies/switzerland/facilities/facilities.xml.gz";
		
		Scenario scenario= GeneralLib.readScenario(inputPlansFile, inputNetworkFile, inputFacilities);
		
		printAllActTypes(scenario.getPopulation());
	}

	
	/**
	 * print summary of all act types used in the plans.
	 * @param population
	 */
	public static void printAllActTypes(Population population){
		LinkedList<String> actTypes=new LinkedList<String>();
		for (Person person:population.getPersons().values()){
			for (Plan plan:person.getPlans()){
				for (PlanElement pe:plan.getPlanElements()){
					if (pe instanceof Activity){
						Activity activity=(Activity) pe;
						if (!actTypes.contains(activity.getType())){
							actTypes.add(activity.getType());
						}
					}
					
				}
			}
		}
		
		for (String acitivityType:actTypes){
			System.out.println(acitivityType);
		}
	}
	
}
