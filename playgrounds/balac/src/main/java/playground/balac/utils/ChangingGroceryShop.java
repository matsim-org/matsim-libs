package playground.balac.utils;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class ChangingGroceryShop {

	public static void main(String[] args) {
		MutableScenario scenario1 = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader populationReader1 = new PopulationReader(scenario1);
		MatsimNetworkReader networkReader1 = new MatsimNetworkReader(scenario1.getNetwork());
		networkReader1.readFile(args[2]);
		populationReader1.readFile(args[0]);
		
		MutableScenario scenario2 = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader populationReader2 = new PopulationReader(scenario2);
		MatsimNetworkReader networkReader2 = new MatsimNetworkReader(scenario2.getNetwork());
		networkReader2.readFile(args[3]);
		populationReader2.readFile(args[1]);
		
		int changed = 0;
		int count = 0;
		int number = 0;
		boolean ind = false;
		for(Person p: scenario1.getPopulation().getPersons().values()) {
			ind = false;
			Plan plan1 = p.getSelectedPlan();
			
			Plan plan2 = scenario2.getPopulation().getPersons().get(p.getId()).getSelectedPlan();
			
			for(PlanElement pe:plan1.getPlanElements()) {
				
				if (pe instanceof Activity) {
					if (((Activity) pe).getType().equals("shopgrocery")) {
						if (!ind) {
							number++;
							ind = true;
						}
						int index = plan1.getPlanElements().indexOf(pe);
						PlanElement pe1 = plan2.getPlanElements().get(index);
						String s1 = ((Activity)pe).getFacilityId().toString();
						String s2 = ((Activity)pe1).getFacilityId().toString();
						if (!s1.contains(s2)) {
							
							changed++;
							
						}
						count++;
					}
				}
			}
			
		}
		
		
		System.out.println(number);
		System.out.println(changed);
		System.out.println(count);
		
	}
	
	
}
