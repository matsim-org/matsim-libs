package org.matsim.modechoice.estimators;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.TripStructureUtils;

public interface LegEstimator {

	// TODO: time ?
	// TODO: affects trips ? PT price or maut
	// state or stateless


	Estimate estimateMin(Person person, String mode, EstimatorState state, TripStructureUtils.Trip trip);

}
