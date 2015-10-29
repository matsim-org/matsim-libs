package playground.balac.utils;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class opla {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(sc);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(sc);
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);
		
		for(Person person: sc.getPopulation().getPersons().values()) {
			if (person.getId().toString().equals("3956576"))
				System.out.println("bla");
			Plan plan = person.getSelectedPlan();
			int count = 0;
			for(PlanElement pe: plan.getPlanElements()) {
				
				if (pe instanceof Leg) {
					
					if (((Leg) pe).getMode().equals("walk_rb")) {
						
						count++;
					}
				}
			}
			
			if (count % 2 != 0) {
				
				System.out.println("bla");
			}
		}

	}

}
