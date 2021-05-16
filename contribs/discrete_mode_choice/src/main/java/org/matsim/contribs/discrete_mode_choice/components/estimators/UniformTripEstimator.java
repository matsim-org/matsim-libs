package org.matsim.contribs.discrete_mode_choice.components.estimators;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.DefaultTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.contribs.discrete_mode_choice.replanning.time_interpreter.TimeInterpreter;

/**
 * This estimator simply return a zero utility for every trip candidate that it
 * sees. Useful for random selection setups.
 * 
 * @author sebhoerl
 */
public class UniformTripEstimator implements TripEstimator {
	private final TimeInterpreter.Factory timeInterpreterFactory;

	public UniformTripEstimator(TimeInterpreter.Factory timeInterpreterFactory) {
		this.timeInterpreterFactory = timeInterpreterFactory;
	}

	@Override
	public TripCandidate estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips) {
		TimeInterpreter time = timeInterpreterFactory.createTimeInterpreter();

		time.setTime(trip.getDepartureTime());
		time.addPlanElements(trip.getInitialElements());

		double duration = time.getCurrentTime() - trip.getDepartureTime();

		return new DefaultTripCandidate(1.0, mode, duration);
	}
}
