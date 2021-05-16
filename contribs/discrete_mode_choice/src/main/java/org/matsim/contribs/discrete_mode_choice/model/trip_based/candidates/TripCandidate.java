package org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates;

import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilityCandidate;

/**
 * A trip candidate represents a potential choice of mode for a specific trip.
 * 
 * @author sebhoerl
 */
public interface TripCandidate extends UtilityCandidate {
	String getMode();
	
	double getDuration();
}
