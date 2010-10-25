package playground.mzilske.controler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;

public class MultiPopulationScenario extends ScenarioImpl {

	Collection<Population> populations = new ArrayList<Population>();
	
	public MultiPopulationScenario(Config config) {
		super(config);
	}

	@Override
	public Population getPopulation() {
		return new Population() {

			@Override
			public void addPerson(Person p) {
				throw new RuntimeException();
			}

			@Override
			public PopulationFactory getFactory() {
				throw new RuntimeException();
			}

			@Override
			public String getName() {
				return "Ulf-Uwe";
			}

			@Override
			public Map<Id, ? extends Person> getPersons() {
				Map<Id, Person> persons = new HashMap<Id, Person>();
				for (Population population : populations) {
					for (Map.Entry<Id, ? extends Person> entry : population.getPersons().entrySet()) {
						Person proxy = new PlanProvidingPersonProxy(entry.getValue());
						persons.put(entry.getKey(), proxy);
					}
				}
				return persons;
			}

			@Override
			public void setName(String name) {
				throw new RuntimeException();
			}
			
		};
	}

	public Collection<Population> getPopulations() {
		return populations;
	}
	
}
