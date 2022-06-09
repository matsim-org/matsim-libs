package org.matsim.modechoice.estimators;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.TripStructureUtils;

public interface StatelessLegEstimator extends LegEstimator {

	Estimate estimateMin(Person person, String mode, TripStructureUtils.Trip trip);

	@Override
	default Estimate estimateMin(Person person, String mode, EstimatorState state, TripStructureUtils.Trip trip) {
		return estimateMin(person, mode, trip);
	}
}
