package org.matsim.contribs.discrete_mode_choice.model.tour_based;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/**
 * Defines a filter that decides whether mode choice should be performed for a
 * specific trip.
 * 
 * @author sebhoerl
 */
public interface TripFilter {
	/**
	 * This function is called before any mode choice attempt for a certain trip is
	 * made. It should return whether the trip should be ignored for decision
	 * making.
	 */
	boolean filter(Person person, DiscreteModeChoiceTrip trip);
}
