package org.matsim.contribs.discrete_mode_choice.model.tour_based;

import java.util.List;

import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

/**
 * Default implementation for a TourCandidate.
 * 
 * @author sebhoerl
 */
public class DefaultTourCandidate implements TourCandidate {
	final private double utility;
	final private List<TripCandidate> tripCandidates;

	public DefaultTourCandidate(double utility, List<TripCandidate> tripCandidates) {
		this.utility = utility;
		this.tripCandidates = tripCandidates;
	}

	@Override
	public double getUtility() {
		return utility;
	}

	@Override
	public List<TripCandidate> getTripCandidates() {
		return tripCandidates;
	}
}
