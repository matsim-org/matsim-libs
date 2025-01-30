package org.matsim.dsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationPartition;

import java.util.Set;

/**
 * Partition to which the {@link DSimControllerListener} will add partition information.
 */
public class LazyPopulationPartition implements PopulationPartition {

	/**
	 * Index will be the rank of the process.
	 */
	private final int index;

	/**
	 * Persons in this partition.
	 */
	private final Set<Id<Person>> persons = new IdSet<>(Person.class);

	public LazyPopulationPartition(int index) {
		this.index = index;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public boolean contains(Id<Person> id) {
		return false;
	}

	void addPerson(Id<Person> id) {
		persons.add(id);
	}

	int size() {
		return persons.size();
	}
}
