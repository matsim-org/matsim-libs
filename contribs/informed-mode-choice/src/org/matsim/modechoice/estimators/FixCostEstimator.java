package org.matsim.modechoice.estimators;

import org.matsim.api.core.v01.population.Person;

public interface FixCostEstimator {

	// estimate the fix cost
	double estimateUtility(Person person, String mode);

}
