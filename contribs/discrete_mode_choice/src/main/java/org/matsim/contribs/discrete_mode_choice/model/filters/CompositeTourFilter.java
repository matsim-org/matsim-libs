package org.matsim.contribs.discrete_mode_choice.model.filters;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourFilter;

/**
 * A tour filter that combines multiple filters. Only if all filters return
 * true, the tour is considered for mode choice.
 * 
 * @author sebhoerl
 */
public class CompositeTourFilter implements TourFilter {
	private final Collection<TourFilter> filters;

	public CompositeTourFilter(Collection<TourFilter> filters) {
		this.filters = filters;
	}

	@Override
	public boolean filter(Person person, List<DiscreteModeChoiceTrip> tour) {
		for (TourFilter filter : filters) {
			if (!filter.filter(person, tour)) {
				return false;
			}
		}

		return true;
	}
}
