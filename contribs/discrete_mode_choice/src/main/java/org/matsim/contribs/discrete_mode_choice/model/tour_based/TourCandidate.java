package org.matsim.contribs.discrete_mode_choice.model.tour_based;

import java.util.List;

import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilityCandidate;

/**
 * Represents a candidate for a whole tour. It consists of a list of trip
 * candidates and may contain additional information.
 * 
 * @author sebhoerl
 */
public interface TourCandidate extends UtilityCandidate {
	List<TripCandidate> getTripCandidates();
}
