package playground.balac.utils.population;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

public class UpdatePopulationForNewScoring {

	public static void main(String[] args) {

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader populationReader = new PopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);

		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			Plan plan = person.getSelectedPlan();
				
			Activity a = (Activity) plan.getPlanElements().get(0);
			
			a.setStartTime(Time.UNDEFINED_TIME);
			int size = plan.getPlanElements().size();
			int i = 0;
			
		/*	for (PlanElement pe : plan.getPlanElements()) {
				
				if (pe instanceof Activity) {
					
					
						if (i != size - 1) {
							((Activity) pe).setStartTime(Time.UNDEFINED_TIME);
							((Activity) pe).setMaximumDuration(Time.UNDEFINED_TIME);
						}
					}
				
				i++;
			}*/
			
		}
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeV4(args[2] + "/plans_10perc_fixedtt_2.xml.gz");		

		
	}

}
