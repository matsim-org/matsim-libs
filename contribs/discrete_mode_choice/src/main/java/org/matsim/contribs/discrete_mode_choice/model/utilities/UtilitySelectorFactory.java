package org.matsim.contribs.discrete_mode_choice.model.utilities;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

/**
 * Creates a UtilitySelector.
 *
 * @author sebhoerl
 */
public interface UtilitySelectorFactory {
	UtilitySelector createUtilitySelector(Person person, List<DiscreteModeChoiceTrip> tourTrips);
}
