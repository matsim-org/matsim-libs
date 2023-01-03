package org.matsim.contribs.discrete_mode_choice.model.mode_availability;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/**
 * This class is used as a filter to define which more are available for a
 * certain chain of trips. Only modes that are returned here are even considered
 * as alternatives for the trips.
 * 
 * @author sebhoerl
 */
public interface ModeAvailability {
	Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips);
}
