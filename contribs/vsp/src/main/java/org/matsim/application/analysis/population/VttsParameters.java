package org.matsim.application.analysis.population;

import com.google.inject.ImplementedBy;
import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

import java.util.List;

/**
 * Conversion values of the scenario's scoring model. The default asks the run's own scoring function, so it
 * follows whatever that function does with money and travel time, including terms outside the scoring parameters.
 * Bind a scenario-specific implementation only if the local slope is not the conversion wanted, for example with
 * a nonlinear money utility. Return NaN for an unsupported conversion to retain MUSE without monetary values.
 */
@ImplementedBy(ScorerVttsParameters.class)
public interface VttsParameters {

	/** Score per money unit for this person; nonpositive or nonfinite values prevent monetization. */
	double marginalUtilityOfMoney(Person person);

	/**
	 * Score per additional hour of traveling in this trip's saving mode, with everything else fixed.
	 * Return NaN when there is no such conversion, rather than substituting zero.
	 */
	double marginalUtilityOfTraveling(Person person, Plan plan, TripStructureUtils.Trip trip, String mode);
}

final class ScorerVttsParameters implements VttsParameters {

	private final ScoringFunctionFactory factory;

	@Inject
	ScorerVttsParameters(ScoringFunctionFactory factory) {
		this.factory = factory;
	}

	@Override
	public double marginalUtilityOfMoney(Person person) {
		ScoringFunction scorer = factory.createNewScoringFunction(person);
		scorer.addMoney(1);
		scorer.finish();
		return scorer.getScore();
	}

	@Override
	public double marginalUtilityOfTraveling(Person person, Plan plan, TripStructureUtils.Trip trip, String mode) {
		Leg leg = trip.getLegsOnly().stream().filter(l -> l.getMode().equals(mode)).findFirst().orElse(null);
		if (leg == null || leg.getTravelTime().isUndefined()) return Double.NaN;
		List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
		double base = replay(person, trips);
		double travelTime = leg.getTravelTime().seconds();
		leg.setTravelTime(travelTime - 1);
		try {
			return (base - replay(person, trips)) * 3600;
		} finally {
			leg.setTravelTime(travelTime);
		}
	}

	/** The day as scoring gets it. Stage activities are left out: they are not scored, and a plan read from file has no times for them. */
	private double replay(Person person, List<TripStructureUtils.Trip> trips) {
		ScoringFunction scorer = factory.createNewScoringFunction(person);
		for (var trip : trips) {
			scorer.handleActivity(trip.getOriginActivity());
			trip.getLegsOnly().forEach(scorer::handleLeg);
			scorer.handleTrip(trip);
		}
		scorer.handleActivity(trips.getLast().getDestinationActivity());
		scorer.finish();
		return scorer.getScore();
	}
}
