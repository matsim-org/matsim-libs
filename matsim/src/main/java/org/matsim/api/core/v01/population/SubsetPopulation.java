package org.matsim.api.core.v01.population;

import org.matsim.api.core.v01.Id;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A population subset using a predicate to filter the persons.
 */
public class SubsetPopulation implements Population {

	private final Population population;
	private final Map<Id<Person>, Person> persons = new LinkedHashMap<>();

	public SubsetPopulation(Population population, Predicate<Id<Person>> contains) {
		this.population = population;

		for (Person person : population.getPersons().values()) {
			if (contains.test(person.getId())) {
				persons.put(person.getId(), person);
			}
		}
	}

	@Override
	public PopulationFactory getFactory() {
		return population.getFactory();
	}

	@Override
	public String getName() {
		return population.getName();
	}

	@Override
	public void setName(String name) {
		population.setName(name);
	}

	@Override
	public Map<Id<Person>, ? extends Person> getPersons() {
		return persons;
	}

	@Override
	public void addPerson(Person p) {
		throw new UnsupportedOperationException("No supported.");
	}

	@Override
	public Person removePerson(Id<Person> personId) {
		throw new UnsupportedOperationException("No supported.");
	}

	@Override
	public Attributes getAttributes() {
		return population.getAttributes();
	}
}
