package playground.sergioo.weeklySetup2016;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class MixPopulations {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scenario).readFile(args[0]);
		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scenario2).readFile(args[1]);
		for(Person person:scenario2.getPopulation().getPersons().values()) {
			Person person2 = scenario2.getPopulation().getPersons().get(person.getId());
			if(person.getSelectedPlan()!=null)
				scenario.getPopulation().getPersons().get(person2.getId()).addPlan(person2.getSelectedPlan());
		}
		new PopulationWriter(scenario.getPopulation()).write(args[2]);
	}

}
