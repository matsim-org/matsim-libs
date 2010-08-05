package playground.wrashid.lib.tools.plan;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.api.core.v01.population.Population;


import playground.wrashid.lib.GeneralLib;


public class RemoveNetworkInformationFromPlan {
	public static void main(String[] args) {
		String inputPlansFile="V:/data/cvs/ivt/studies/switzerland/plans/ivtch/census2000v2_dilZh30km_10pct/plans.xml.gz";
		String inputNetworkFile="V:/data/cvs/ivt/studies/switzerland/networks/ivtch/network.xml";
		String inputFacilities="V:/data/cvs/ivt/studies/switzerland/facilities/facilities.xml.gz";
		
		String outputPlansFile="v:/data/v-temp/plans-new.xml.gz";		
		
		Scenario scenario= GeneralLib.readPopulation(inputPlansFile, inputNetworkFile,inputFacilities);
		
		removeNetworkInformationFromPlans(scenario.getPopulation());

		
		GeneralLib.writePopulation(scenario.getPopulation(),scenario.getNetwork(),outputPlansFile);
	}
	
	
	/**
	 * Sometimes the plans contain information related to the network and can therefore not be used with a different network.
	 * This method removes such information.
	 */
	public static void removeNetworkInformationFromPlans(Population population){
		for (Person person:population.getPersons().values()){
			for (Plan plan:person.getPlans()){
				for (PlanElement pe:plan.getPlanElements()){
					if (pe instanceof Activity){
						ActivityImpl activity=(ActivityImpl) pe;
						activity.setLinkId(null);
						//activity.setFacilityId(null);
					}
					if (pe instanceof Leg){
						LegImpl leg=(LegImpl) pe;
						leg.setRoute(null);
					}
				}
			}
		}
	}
}
