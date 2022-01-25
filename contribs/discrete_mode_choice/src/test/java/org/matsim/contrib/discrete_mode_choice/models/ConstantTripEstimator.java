package org.matsim.contrib.discrete_mode_choice.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.nested.DefaultNestedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.nested.Nest;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

public class ConstantTripEstimator implements TripEstimator {
	private final Map<String, Double> utilities = new HashMap<>();
	private final Map<String, Nest> nests = new HashMap<>();

	public void setAlternative(String mode, double utility, Nest nest) {
		utilities.put(mode, utility);
		nests.put(mode, nest);
	}

	public void setAlternative(String mode, double utility) {
		setAlternative(mode, utility, null);
	}

	@Override
	public TripCandidate estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips) {
		return new DefaultNestedTripCandidate(utilities.get(mode), mode, 0.0, nests.get(mode));
	}
}