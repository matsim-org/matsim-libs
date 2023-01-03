package org.matsim.contribs.discrete_mode_choice.model.trip_based;

import java.util.List;

import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

/**
 * This class defines a constriant for a trip.
 * 
 * @author sebhoerl
 */
public interface TripConstraint {
	/**
	 * This function is called before any estimation of utilities for a trip is
	 * happening. If it returns true, the given mode is a feasible option (at least
	 * before estimating the routing etc.) for the trip. If it returns false, the
	 * given mode will not be considered further as an alternative for the trip.
	 * 
	 * Since trip decisions are performed one after another the previousModes
	 * argument contains a list of modes that have been chosen for earlier trips in
	 * the plan.
	 */
	boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes);

	/**
	 * This function is called after a trip is estimated. If it returns true, the
	 * given mode is a feasible option for the trip, while false indicates that this
	 * mode alternative should not be considered anymore.
	 * 
	 * Since trip decisions are performed one after another the previousCandidates
	 * argument contains a list of candidates that have been chosen for earlier
	 * trips in the plan.
	 */
	boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate,
			List<TripCandidate> previousCandidates);
}
