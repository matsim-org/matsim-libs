package org.matsim.contribs.discrete_mode_choice.model.tour_based;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/**
 * Estimates the utility of a whole tour with given trips and modes with which
 * they should be performed.
 * 
 * @author sebhoerl
 */
public interface TourEstimator {
	TourCandidate estimateTour(Person person, List<String> modes, List<DiscreteModeChoiceTrip> trips,
			List<TourCandidate> previousTours);
}
