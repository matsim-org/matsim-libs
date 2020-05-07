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
import org.matsim.contribs.discrete_mode_choice.replanning.time_interpreter.TimeInterpreter;

/**
 * This estimator simply return a zero utility for every tour candidate that it
 * sees. Useful for random selection setups.
 * 
 * @author sebhoerl
 */
public class UniformTourEstimator implements TourEstimator {
	private final TimeInterpreter.Factory timeInterpreterFactory;

	public UniformTourEstimator(TimeInterpreter.Factory timeInterpreterFactory) {
		this.timeInterpreterFactory = timeInterpreterFactory;
	}

	@Override
	public TourCandidate estimateTour(Person person, List<String> modes, List<DiscreteModeChoiceTrip> trips,
			List<TourCandidate> previousTours) {
		List<TripCandidate> tripCandidates = new ArrayList<>(modes.size());

		TimeInterpreter time = timeInterpreterFactory.createTimeInterpreter();
		time.setTime(trips.get(0).getDepartureTime());

		for (int index = 0; index < modes.size(); index++) {
			DiscreteModeChoiceTrip trip = trips.get(index);

			if (index > 0) { // We're already at the end of the first origin activity
				time.addActivity(trip.getOriginActivity());
				trip.setDepartureTime(time.getCurrentTime());
			}

			time.addPlanElements(trip.getInitialElements());

			double duration = time.getCurrentTime() - trip.getDepartureTime();
			tripCandidates.add(new DefaultTripCandidate(1.0, modes.get(index), duration));
		}

		return new DefaultTourCandidate(1.0, tripCandidates);
	}
}
