package org.matsim.contribs.discrete_mode_choice.model.filters;

import java.util.Collection;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TripFilter;

/**
 * A trip filter that combines multiple filters. Only if all filters return
 * true, the trip is considered for mode choice.
 * 
 * @author sebhoerl
 */
public class CompositeTripFilter implements TripFilter {
	private final Collection<TripFilter> filters;

	public CompositeTripFilter(Collection<TripFilter> filters) {
		this.filters = filters;
	}

	@Override
	public boolean filter(Person person, DiscreteModeChoiceTrip trip) {
		for (TripFilter filter : filters) {
			if (!filter.filter(person, trip)) {
				return false;
			}
		}

		return true;
	}
}
