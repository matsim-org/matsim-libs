package org.matsim.contribs.discrete_mode_choice.components.filters;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourFilter;

public class TourLengthFilter implements TourFilter {
	private final int maximumLength;

	public TourLengthFilter(int maximumLength) {
		this.maximumLength = maximumLength;
	}

	@Override
	public boolean filter(Person person, List<DiscreteModeChoiceTrip> tour) {
		return tour.size() <= maximumLength;
	}
}
