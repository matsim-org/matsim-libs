package org.matsim.contribs.discrete_mode_choice.model.estimation;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

/**
 * An abstract helper class that removes the mode dependency of the trip
 * estimator. This means that the estimator will be responsible for one specific
 * mode.
 * 
 * @author sebhoerl
 */
public abstract class ModalTripEstimator implements TripEstimator {
	abstract protected TripCandidate estimateTrip(Person person, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips);

	@Override
	public TripCandidate estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips) {
		return estimateTrip(person, trip, previousTrips);
	}
}
