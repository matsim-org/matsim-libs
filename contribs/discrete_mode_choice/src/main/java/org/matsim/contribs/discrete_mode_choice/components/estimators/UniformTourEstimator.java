package org.matsim.contribs.discrete_mode_choice.components.estimators;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.DefaultTourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.DefaultTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

/**
 * This estimator simply return a zero utility for every tour candidate that it
 * sees. Useful for random selection setups.
 * 
 * @author sebhoerl
 */
public class UniformTourEstimator implements TourEstimator {
	private final TimeInterpretation timeInterpretation;

	public UniformTourEstimator(TimeInterpretation timeInterpretation) {
		this.timeInterpretation = timeInterpretation;
	}

	@Override
	public TourCandidate estimateTour(Person person, List<String> modes, List<DiscreteModeChoiceTrip> trips,
			List<TourCandidate> previousTours) {
		List<TripCandidate> tripCandidates = new ArrayList<>(modes.size());

		TimeTracker timeTracker = new TimeTracker(timeInterpretation);
		timeTracker.setTime(trips.get(0).getDepartureTime());

		for (int index = 0; index < modes.size(); index++) {
			DiscreteModeChoiceTrip trip = trips.get(index);

			if (index > 0) { // We're already at the end of the first origin activity
				timeTracker.addActivity(trip.getOriginActivity());
				trip.setDepartureTime(timeTracker.getTime().seconds());
			}

			timeTracker.addElements(trip.getInitialElements());

			double duration = timeTracker.getTime().seconds() - trip.getDepartureTime();
			tripCandidates.add(new DefaultTripCandidate(1.0, modes.get(index), duration));
		}

		return new DefaultTourCandidate(1.0, tripCandidates);
	}
}
