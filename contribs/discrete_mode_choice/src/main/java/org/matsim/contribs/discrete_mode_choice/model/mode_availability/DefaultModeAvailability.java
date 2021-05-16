package org.matsim.contribs.discrete_mode_choice.model.mode_availability;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/**
 * This implementation of ModeAvailability has a static list of modes that will
 * be used as alternatives.
 * 
 * @author sebhoerl
 */
public class DefaultModeAvailability implements ModeAvailability {
	final private Collection<String> modes;

	public DefaultModeAvailability(Collection<String> modes) {
		this.modes = modes;
	}

	@Override
	public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
		return modes;
	}
}
