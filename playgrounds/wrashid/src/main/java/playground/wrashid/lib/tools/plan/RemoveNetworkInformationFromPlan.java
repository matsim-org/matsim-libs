package playground.wrashid.lib.tools.plan;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;

import playground.wrashid.lib.GeneralLib;


public class RemoveNetworkInformationFromPlan {
	public static void main(String[] args) {
		
		String inputPlansFile="C:/eTmp/census2000v2_8kmCut_10pct_no_network_info.xml.gz";
		String inputNetworkFile="P:/Projekte/matsim/data/switzerland/networks/teleatlas-ivtcheu/network.xml.gz";
		String inputFacilities="P:/Projekte/matsim/data/switzerland/facilities/facilities.xml.gz";
		
		
		String outputPlansFile="H:/data/experiments/TRBAug2012/input/census2000v2_ZhCut8km_10pct_no_network_info.xml.gz";		
		
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
