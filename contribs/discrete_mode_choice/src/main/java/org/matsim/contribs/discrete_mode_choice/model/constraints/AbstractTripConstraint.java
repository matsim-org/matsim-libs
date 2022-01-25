package org.matsim.contribs.discrete_mode_choice.model.constraints;

import java.util.List;

import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

/**
 * An abstract TripConstraint that always returns true if the validation methods
 * are not overridden.
 * 
 * @author sebhoerl
 */
public abstract class AbstractTripConstraint implements TripConstraint {
	public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
		return true;
	}

	public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate,
			List<TripCandidate> previousCandidates) {
		return true;
	}
}
