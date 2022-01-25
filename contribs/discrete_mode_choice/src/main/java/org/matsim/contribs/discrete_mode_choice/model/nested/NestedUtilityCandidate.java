package org.matsim.contribs.discrete_mode_choice.model.nested;

import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilityCandidate;

public interface NestedUtilityCandidate extends UtilityCandidate {
	Nest getNest();
}