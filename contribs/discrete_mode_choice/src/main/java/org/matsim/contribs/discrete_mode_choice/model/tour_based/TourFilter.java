package org.matsim.contribs.discrete_mode_choice.model.tour_based;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/**
 * Defines a filter that decides whether mode choice should be performed for a
 * specific tour.
 * 
 * @author sebhoerl
 */
public interface TourFilter {
	/**
	 * This function is called before any mode choice attempt for a certain tour is
	 * made. It should return whether the tour should be ignored for decision
	 * making.
	 */
	boolean filter(Person person, List<DiscreteModeChoiceTrip> tour);
}
