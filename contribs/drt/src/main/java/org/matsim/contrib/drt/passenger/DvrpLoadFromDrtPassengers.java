package org.matsim.contrib.drt.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpLoad;

import java.util.Collection;

public interface DvrpLoadFromDrtPassengers {

	DvrpLoad getLoad(Collection<Id<Person>> personIds);
}
