package org.matsim.contrib.drt.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.dvrp_load.DvrpLoad;

import java.util.Collection;

/**
 * This interface is to be bound per Drt mode. It allows to construct the {@link DvrpLoad} object representing the occupancy of a DrtRequest.
 * Implementations must implement the {@link DvrpLoadFromDrtPassengers#getLoad(Collection)} method that computes the load from the passenger IDs.
 * @author Tarek Chouaki (tkchouaki)
 */
public interface DvrpLoadFromDrtPassengers {
	DvrpLoad getLoad(Collection<Id<Person>> personIds);
}
