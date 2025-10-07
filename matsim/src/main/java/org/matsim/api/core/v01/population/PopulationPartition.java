package org.matsim.api.core.v01.population;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Partition;

/**
 * Interface to be used for bindings.
 */
public interface PopulationPartition extends Partition<Person> {

	/**
	 * A partition that contains all persons.
	 */
	PopulationPartition SINGLE_INSTANCE = new PopulationPartition() {
		@Override
		public int getIndex() {
			return 0;
		}

		@Override
		public boolean contains(Id<Person> id) {
			return true;
		}
	};

}
