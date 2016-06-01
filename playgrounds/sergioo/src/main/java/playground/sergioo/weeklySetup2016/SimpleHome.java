package playground.sergioo.weeklySetup2016;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class SimpleHome {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scenario).readFile(args[0]);
		Set<Id<Person>> toDelete = new HashSet<>();
		for(Person person:scenario.getPopulation().getPersons().values()) {
			if(person.getSelectedPlan()!=null) {
				for(PlanElement planElement:person.getSelectedPlan().getPlanElements())
					if(planElement instanceof Activity && ((Activity)planElement).getType().startsWith("home"))
						((Activity)planElement).setType("home");
			}
			else {
				toDelete.add(person.getId());
			}
		}
		System.out.println(toDelete.size());
		for(Id<Person> id:toDelete)
			scenario.getPopulation().getPersons().remove(id);
		new PopulationWriter(scenario.getPopulation()).write(args[1]);
	}

}
