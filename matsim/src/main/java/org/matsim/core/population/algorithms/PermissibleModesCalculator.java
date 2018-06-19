package org.matsim.core.population.algorithms;

import java.util.Collection;

import org.matsim.api.core.v01.population.Plan;

public interface PermissibleModesCalculator {
	
	/**
	 * @param plan
	 * @return Collection of modes that the agent can in principle use.  For example, cannot use car if no car is available;
	 * cannot use car sharing if not member.
	 */
	Collection<String> getPermissibleModes(Plan plan);

}
