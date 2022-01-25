package org.matsim.contribs.discrete_mode_choice.model.constraints;

import java.util.List;

import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraint;

/**
 * A TourConstraint that makes it easy to combine different constraints.
 * 
 * Validation happens as a AND operation, i.e. a candidate is only considered
 * feasible if all child constraints find it feasible.
 * 
 * @author sebhoerl
 */
public class CompositeTourConstraint implements TourConstraint {
	final private List<TourConstraint> constraints;

	CompositeTourConstraint(List<TourConstraint> constraints) {
		this.constraints = constraints;
	}

	@Override
	public boolean validateBeforeEstimation(List<DiscreteModeChoiceTrip> tour, List<String> modes,
			List<List<String>> previousModes) {
		for (TourConstraint constraint : constraints) {
			if (!constraint.validateBeforeEstimation(tour, modes, previousModes)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean validateAfterEstimation(List<DiscreteModeChoiceTrip> tour, TourCandidate candidate,
			List<TourCandidate> previousCandidates) {
		for (TourConstraint constraint : constraints) {
			if (!constraint.validateAfterEstimation(tour, candidate, previousCandidates)) {
				return false;
			}
		}

		return true;
	}
}
