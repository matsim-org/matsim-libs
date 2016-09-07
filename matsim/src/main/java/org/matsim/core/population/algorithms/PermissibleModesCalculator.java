package org.matsim.core.population.algorithms;

import java.util.Collection;

import org.matsim.api.core.v01.population.Plan;

public interface PermissibleModesCalculator {

	Collection<String> getPermissibleModes(Plan plan);

}
