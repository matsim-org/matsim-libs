package org.matsim.contribs.discrete_mode_choice.model.constraints;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraint;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;

/**
 * Creates a TourFromTripConstraint.
 * 
 * @author sebhoerl
 */
public class TourFromTripConstraintFactory implements TourConstraintFactory {
	private final TripConstraintFactory factory;

	public TourFromTripConstraintFactory(TripConstraintFactory factory) {
		this.factory = factory;
	}

	@Override
	public TourConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
			Collection<String> availableModes) {
		TripConstraint constraint = factory.createConstraint(person, planTrips, availableModes);
		return new TourFromTripConstraint(constraint);
	}
}
