package playground.balac.utils;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class CompareScenarios {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		MutableScenario scenario1 = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader populationReader1 = new PopulationReader(scenario1);
		MatsimNetworkReader networkReader1 = new MatsimNetworkReader(scenario1.getNetwork());
		networkReader1.readFile(args[0]);
		populationReader1.readFile(args[1]);
		
		MutableScenario scenario2 = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader populationReader2 = new PopulationReader(scenario2);
		MatsimNetworkReader networkReader2 = new MatsimNetworkReader(scenario2.getNetwork());
		networkReader2.readFile(args[0]);
		populationReader2.readFile(args[2]);
		
		int car = 0;
		int pt = 0;
		int walk = 0;
		int bike = 0;
		for (Person p : scenario2.getPopulation().getPersons().values()) {
			
			for(PlanElement pe : scenario1.getPopulation().getPersons().get(p.getId()).getSelectedPlan().getPlanElements()) {
				
				if (pe instanceof Leg) {
					
					if (((Leg) pe).getMode().equals("car"))
						car++;
					else if (((Leg) pe).getMode().equals("pt"))
						pt++;
					else if (((Leg) pe).getMode().equals("walk"))
						walk++;
					else if (((Leg) pe).getMode().equals("bike"))
						bike++;
					
				}
			}
			
		}
		
		System.out.println(car + " " + pt + " " + bike + " " + walk);
		
		
	}

}
