package org.matsim.contribs.discrete_mode_choice.components.estimators;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.DefaultTourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

/**
 * This class is a TourEstimator which is based on a TripEstimator. Every trip
 * in the tour is estimated by the underlying TripEstimator and utilities are
 * summed up to arrive at a total utility for the whole tour.
 * 
 * @author sebhoerl
 */
public class CumulativeTourEstimator implements TourEstimator {
	private final TimeInterpretation timeInterpretation;
	private final TripEstimator delegate;

	public CumulativeTourEstimator(TripEstimator delegate, TimeInterpretation timeInterpretation) {
		this.delegate = delegate;
		this.timeInterpretation = timeInterpretation;
	}

	@Override
	public TourCandidate estimateTour(Person person, List<String> modes, List<DiscreteModeChoiceTrip> trips,
			List<TourCandidate> preceedingTours) {
		List<TripCandidate> tripCandidates = new LinkedList<>();
		double utility = 0.0;

		TimeTracker timeTracker = new TimeTracker(timeInterpretation);
		timeTracker.setTime(trips.get(0).getDepartureTime());

		for (int i = 0; i < modes.size(); i++) {
			String mode = modes.get(i);
			DiscreteModeChoiceTrip trip = trips.get(i);

			if (i > 0) { // We're already at the end of the first origin activity
				timeTracker.addActivity(trip.getOriginActivity());
				trip.setDepartureTime(timeTracker.getTime().seconds());
			}

			TripCandidate tripCandidate = delegate.estimateTrip(person, mode, trip, tripCandidates);
			utility += tripCandidate.getUtility();
			timeTracker.addDuration(tripCandidate.getDuration());

			tripCandidates.add(tripCandidate);
		}

		return new DefaultTourCandidate(utility, tripCandidates);
	}
}
