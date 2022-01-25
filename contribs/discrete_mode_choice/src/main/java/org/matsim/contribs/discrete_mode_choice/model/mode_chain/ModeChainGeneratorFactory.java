package org.matsim.contribs.discrete_mode_choice.model.mode_chain;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/**
 * Creates a ModeChainGenerator.
 * 
 * @author sebhoerl
 */
public interface ModeChainGeneratorFactory {
	ModeChainGenerator createModeChainGenerator(Collection<String> availableModes, Person person,
			List<DiscreteModeChoiceTrip> trips);
}
