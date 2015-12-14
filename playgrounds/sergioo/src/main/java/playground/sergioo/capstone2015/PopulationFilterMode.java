package playground.sergioo.capstone2015;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class PopulationFilterMode {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scenario).readFile(args[0]);
		int cars = 0, passengers = 0, pts=0;
		Set<Id<Person>> deleted = new HashSet<>();
		for(Person person:scenario.getPopulation().getPersons().values()) {
			boolean car = false;
			for(PlanElement planElement:person.getSelectedPlan().getPlanElements())
				if(planElement instanceof Leg && ((Leg)planElement).getMode().equals("car"))
					car = true;
			if(car)
				cars++;
			boolean passenger = false;
			for(PlanElement planElement:person.getSelectedPlan().getPlanElements()) {
				if(planElement instanceof Leg && ((Leg)planElement).getMode().equals("passenger"))
					passenger = true;
			}
			if(passenger) {
				passengers++;
				deleted.add(person.getId());
			}
			if(!car && !passenger && Math.random()<0.150902679/0.7866622)
				deleted.add(person.getId());
		}
		for(Id<Person> id:deleted)
			scenario.getPopulation().getPersons().remove(id);
		new PopulationWriter(scenario.getPopulation()).write(args[1]);
		System.out.println(scenario.getPopulation().getPersons().size());
		System.out.println(cars+"/"+scenario.getPopulation().getPersons().size()+" "+cars*100.0/scenario.getPopulation().getPersons().size());
		System.out.println(passengers+"/"+scenario.getPopulation().getPersons().size()+" "+passengers*100.0/scenario.getPopulation().getPersons().size());
		System.out.println(pts+"/"+scenario.getPopulation().getPersons().size()+" "+pts*100.0/scenario.getPopulation().getPersons().size());
	}

}
