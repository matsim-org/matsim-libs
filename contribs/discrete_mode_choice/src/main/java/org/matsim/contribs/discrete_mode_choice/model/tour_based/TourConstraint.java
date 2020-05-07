package org.matsim.contribs.discrete_mode_choice.model.tour_based;

import java.util.List;

import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/**
 * Defines a constraint on the tour level.
 * 
 * @author sebhoerl
 */
public interface TourConstraint {
	/**
	 * This function is called before any estimation of utilities for a tour is
	 * happening. If it returns true, the given modes are a feasible option (at
	 * least before estimating the routing etc.) for the tour. If it returns false,
	 * the given modes will not be considered further as an alternative for the
	 * tour.
	 * 
	 * Since tour decisions are performed one after another the previousModes
	 * argument contains a list of modes that have been chosen for earlier tours in
	 * the plan.
	 */
	boolean validateBeforeEstimation(List<DiscreteModeChoiceTrip> tour, List<String> modes,
			List<List<String>> previousModes);

	/**
	 * This function is called after a tour is estimated. If it returns true, the
	 * given modes are a feasible option for the tour, while false indicates that
	 * these mode alternatives should not be considered anymore.
	 * 
	 * Since tour decisions are performed one after another the previousCandidates
	 * argument contains a list of candidates that have been chosen for earlier
	 * tours in the plan.
	 */
	boolean validateAfterEstimation(List<DiscreteModeChoiceTrip> tour, TourCandidate candidate,
			List<TourCandidate> previousCandidates);
}
