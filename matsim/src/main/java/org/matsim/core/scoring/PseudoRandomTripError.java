package org.matsim.core.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.TripStructureUtils;

/**
 * Interface to provide pseudo-random errors for a trip.
 */
public interface PseudoRandomTripError {

	/**
	 * Return a seed for a trip. The seed must be designed such that it is constant for the same choice situations.
	 */
	long getSeed(Id<Person> personId, String mainMode, TripStructureUtils.Trip trip);


}
