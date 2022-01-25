package org.matsim.contribs.discrete_mode_choice.model.estimation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

/**
 * A TripEstimator which delegates requests for a certain mode to specific
 * TripEstimators.
 * 
 * @author sebhoerl
 */
public class ModeAwareTripEstimator implements TripEstimator {
	final private Map<String, TripEstimator> modalTripEstimators = new HashMap<>();

	public void addEstimator(String mode, TripEstimator estimator) {
		modalTripEstimators.put(mode, estimator);
	}

	@Override
	public TripCandidate estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> preceedingTrips) {
		TripEstimator delegate = modalTripEstimators.get(mode);

		if (delegate != null) {
			TripCandidate candidate = delegate.estimateTrip(person, mode, trip, preceedingTrips);

			if (!candidate.getMode().equals(mode)) {
				throw new IllegalArgumentException(
						String.format("Expected mode '%s' instead of '%s' to be returned by %s", mode,
								candidate.getMode(), delegate.getClass().toString()));
			}

			return candidate;
		}

		throw new IllegalArgumentException(String.format("No estimator found for mode '%s'", mode));
	}
}
