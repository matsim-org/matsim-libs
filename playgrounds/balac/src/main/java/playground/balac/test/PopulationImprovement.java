package playground.balac.test;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class PopulationImprovement {

	public static void main(String[] args) {

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader populationReader = new PopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(args[1]);
		populationReader.readFile(args[0]);
		
		
		for (Person person: scenario.getPopulation().getPersons().values()) {
			
			Plan plan = person.getSelectedPlan();
			
			List<Activity> t = TripStructureUtils.getActivities(plan, null);
			boolean ind = false;
			for (Activity a : t) {
				if (a.getType().equals("secondary"))
					ind = true;
			}
			
			if (ind) {
				
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {
						
						if (((Activity) pe).getType().equals("home"))
							((Activity) pe).setType("home_2");
						
					}
				}
			}
			else {
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {
						
						if (((Activity) pe).getType().equals("home"))
							((Activity) pe).setType("home_1");
						
					}
				}
				
			}
			
		}
		
		new PopulationWriter(scenario.getPopulation(), 
				scenario.getNetwork()).writeV4("C:\\Users\\balacm\\Desktop\\population_improved.xml.gz");	

		
	}

}
