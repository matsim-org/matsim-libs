package org.matsim.contribs.discrete_mode_choice.components.estimators;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.DefaultRoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

/**
 * This is an abstract estimator class that makes it easy to rely on MATSim's
 * TripRouter. Instead of just getting a proposed mode, this class already
 * routes the trip with the given mode in the background. All that remains is to
 * analyze the PlanElements to estimate a utility.
 * 
 * @author sebhoerl
 */
public abstract class AbstractTripRouterEstimator implements TripEstimator {
	private final TripRouter tripRouter;
	private final ActivityFacilities facilities;
	private final TimeInterpretation timeInterpretation;
	private final Collection<String> preroutedModes;

	public AbstractTripRouterEstimator(TripRouter tripRouter, ActivityFacilities facilities,
			TimeInterpretation timeInterpretation, Collection<String> preroutedModes) {
		this.tripRouter = tripRouter;
		this.facilities = facilities;
		this.timeInterpretation = timeInterpretation;
		this.preroutedModes = preroutedModes;
	}

	private boolean isPrerouted(String mode, DiscreteModeChoiceTrip trip) {
		if (preroutedModes.contains(mode)) {
			if (mode.equals(trip.getInitialMode())) {
				Leg initialLeg = (Leg) trip.getInitialElements().get(0);
				double initialDepartureTime = initialLeg.getDepartureTime().seconds();

				if (initialDepartureTime == trip.getDepartureTime()) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public final TripCandidate estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips) {
		// I) Find the correct origin and destination facilities

		Facility originFacility = FacilitiesUtils.toFacility(trip.getOriginActivity(), facilities);
		Facility destinationFacility = FacilitiesUtils.toFacility(trip.getDestinationActivity(), facilities);

		if (!isPrerouted(mode, trip)) {
			// II) Perform the routing
			List<? extends PlanElement> elements = tripRouter.calcRoute(mode, originFacility, destinationFacility,
					trip.getDepartureTime(), person, trip.getTripAttributes());

			// III) Perform utility estimation
			return estimateTripCandidate(person, mode, trip, previousTrips, elements);
		} else {
			// If we already have the route of interest, just pass it on
			return estimateTripCandidate(person, mode, trip, previousTrips, trip.getInitialElements());
		}
	}

	/**
	 * Implement this if you just want to calculate a utility, but don't want to
	 * return a custom TripCandidate object.
	 */
	protected double estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips, List<? extends PlanElement> routedTrip) {
		return 0.0;
	}

	/**
	 * Implement this if you want to return a custom TripCandidate object rather
	 * than just a utility.
	 */
	protected TripCandidate estimateTripCandidate(Person person, String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips, List<? extends PlanElement> routedTrip) {

		TimeTracker timeTracker = new TimeTracker(timeInterpretation);
		timeTracker.setTime(trip.getDepartureTime());
		timeTracker.addElements(routedTrip);

		double utility = estimateTrip(person, mode, trip, previousTrips, routedTrip);

		double duration = timeTracker.getTime().seconds() - trip.getDepartureTime();
		return new DefaultRoutedTripCandidate(utility, mode, routedTrip, duration);
	}
}
