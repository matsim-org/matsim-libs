package org.matsim.contribs.discrete_mode_choice.components.tour_finder;

import java.util.Collections;
import java.util.List;

import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/**
 * This TourFinder simply defines the whole plan of an agent as one single tour.
 * 
 * @author sebhoerl
 */
public class PlanTourFinder implements TourFinder {
	@Override
	public List<List<DiscreteModeChoiceTrip>> findTours(List<DiscreteModeChoiceTrip> trips) {
		return Collections.singletonList(trips);
	}
}
