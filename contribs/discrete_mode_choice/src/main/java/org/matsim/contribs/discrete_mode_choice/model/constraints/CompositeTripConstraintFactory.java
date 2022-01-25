package org.matsim.contribs.discrete_mode_choice.model.constraints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;

/**
 * Creates a CompositeTripConstraint.
 * 
 * @author sebhoerl
 */
public class CompositeTripConstraintFactory implements TripConstraintFactory {
	final private List<TripConstraintFactory> factories = new LinkedList<>();

	public CompositeTripConstraintFactory() {
	}

	public CompositeTripConstraintFactory(List<TripConstraintFactory> factories) {
		this.factories.addAll(factories);
	}

	public void addFactory(TripConstraintFactory factory) {
		this.factories.add(factory);
	}

	@Override
	public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
			Collection<String> availableModes) {
		List<TripConstraint> constraints = new ArrayList<>(factories.size());
		factories.forEach(f -> constraints.add(f.createConstraint(person, planTrips, availableModes)));
		return new CompositeTripConstraint(constraints);
	}
}
