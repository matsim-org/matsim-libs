package org.matsim.contribs.discrete_mode_choice.model.constraints;

import java.util.List;

import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

/**
 * A TripConstraint that makes it easy to combine different constraints.
 * 
 * Validation happens as a AND operation, i.e. a candidate is only considered
 * feasible if all child constraints find it feasible.
 * 
 * @author sebhoerl
 */
public class CompositeTripConstraint implements TripConstraint {
	final private List<TripConstraint> constraints;

	CompositeTripConstraint(List<TripConstraint> constraints) {
		this.constraints = constraints;
	}

	@Override
	public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
		for (TripConstraint constraint : constraints) {
			if (!constraint.validateBeforeEstimation(trip, mode, previousModes)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate,
			List<TripCandidate> previousCandidates) {
		for (TripConstraint constraint : constraints) {
			if (!constraint.validateAfterEstimation(trip, candidate, previousCandidates)) {
				return false;
			}
		}

		return true;
	}
}
