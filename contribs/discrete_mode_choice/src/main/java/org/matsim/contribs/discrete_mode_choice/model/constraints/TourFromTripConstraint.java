package org.matsim.contribs.discrete_mode_choice.model.constraints;

import java.util.LinkedList;
import java.util.List;

import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

/**
 * Defines a constraint that applies trip-based constraint on the tour level.
 * This means that the trip constraint must be fulfilled for each trip in the
 * tour for the tour to be feasible.
 * 
 * @author sebhoerl
 */
public class TourFromTripConstraint implements TourConstraint {
	private final TripConstraint constraint;

	TourFromTripConstraint(TripConstraint constraint) {
		this.constraint = constraint;
	}

	@Override
	public boolean validateBeforeEstimation(List<DiscreteModeChoiceTrip> currentTourTrips,
			List<String> currentTourModes, List<List<String>> previousTourModes) {
		List<String> previousTripModes = new LinkedList<>();
		previousTourModes.forEach(previousTripModes::addAll);

		for (int i = 0; i < currentTourModes.size(); i++) {
			if (!constraint.validateBeforeEstimation(currentTourTrips.get(i), currentTourModes.get(i),
					previousTripModes)) {
				return false;
			}

			previousTripModes.add(currentTourModes.get(i));
		}

		return true;
	}

	@Override
	public boolean validateAfterEstimation(List<DiscreteModeChoiceTrip> currentTourTrips,
			TourCandidate currentTourCandidate, List<TourCandidate> previousTourCandidates) {
		List<TripCandidate> previousTripCandidates = new LinkedList<>();
		previousTourCandidates.stream().map(TourCandidate::getTripCandidates).forEach(previousTripCandidates::addAll);

		for (int i = 0; i < currentTourCandidate.getTripCandidates().size(); i++) {
			TripCandidate currentTripCandidate = currentTourCandidate.getTripCandidates().get(i);

			if (!constraint.validateAfterEstimation(currentTourTrips.get(i), currentTripCandidate,
					previousTripCandidates)) {
				return false;
			}

			previousTripCandidates.add(currentTripCandidate);
		}

		return true;
	}
}
