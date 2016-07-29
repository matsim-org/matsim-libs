package playground.wrashid.lib.tools.plan;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.population.PersonUtils;

public class CleanKTIPlan {

	public static void main(String[] args) {

		String inputPlansFile="C:/data/parkingSearch/zurich/input/10pct_plans_kti.xml.gz";
		String inputNetworkFile="C:/data/parkingSearch/zurich/input/network.xml.gz";
		String inputFacilities="C:/data/parkingSearch/zurich/input/facilities.xml.gz";

		String outputPlansFile = "C:/data/parkingSearch/zurich/input/10pct_plans_ktiClean.xml.gz";

		Scenario scenario = GeneralLib.readScenario(inputPlansFile, inputNetworkFile, inputFacilities);

		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			Person p=(Person) person;
			PersonUtils.removeUnselectedPlans(p);
			
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Activity) {
					
				}
				
				if (pe instanceof Leg) {
					Leg leg=(Leg) pe;
					if (leg.getMode().equalsIgnoreCase("pt")){
						leg.setRoute(null);
					}
				}
			}
		}
		
		GeneralLib.writePopulation(scenario.getPopulation(), scenario.getNetwork(), outputPlansFile);

	}

}
