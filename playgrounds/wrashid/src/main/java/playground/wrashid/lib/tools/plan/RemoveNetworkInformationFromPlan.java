package playground.wrashid.lib.tools.plan;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;



public class RemoveNetworkInformationFromPlan {
	public static void main(String[] args) {
		
		String inputPlansFile= "C:/data/parkingSearch/zurich/input/10pct_dil_1km_plans_ktiClean.xml.gz";
		String inputNetworkFile="P:/Projekte/matsim/data/switzerland/networks/teleatlas-ivtcheu/network.xml.gz";
		String inputFacilities="P:/Projekte/matsim/data/switzerland/facilities/facilities.xml.gz";
		
		
		String outputPlansFile="C:/data/parkingSearch/zurich/input/10pct_dil_1km_plans_ktiClean_noNetworkInfo.xml.gz";		
		
		Scenario scenario= GeneralLib.readScenario(inputPlansFile, inputNetworkFile,inputFacilities);
		
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
