package org.matsim.contribs.discrete_mode_choice.model.tour_based;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/**
 * Creates a tour level constraint.
 * 
 * @author sebhoerl
 */
public interface TourConstraintFactory {
	TourConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
			Collection<String> availableModes);
}