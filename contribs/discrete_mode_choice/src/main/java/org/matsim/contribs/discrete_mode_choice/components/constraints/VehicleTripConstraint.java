package org.matsim.contribs.discrete_mode_choice.components.constraints;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.components.utils.LocationUtils;
import org.matsim.contribs.discrete_mode_choice.components.utils.home_finder.HomeFinder;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

/**
 * This is a vehicle constraint (see VehicleTourConstraint), but on a trip
 * level. Here the case is more difficult than on the tour-level and not all of
 * the dynamic can be enforced. Have a look at the code for the exact
 * implementation.
 * 
 * Attention! This is not tested and may be faulty. Feel free to add some test
 * cases to see if this actually does what it is supposed to do.
 * 
 * TODO: Revise this and check if it makes sense!
 * 
 * This class ensures that the vehicle needs to be returned home if it was taken
 * in the first place. However, it must be noted that if the person start a tour
 * with a certain vehicle it needs to bring it back at the end of the tour. This
 * means that if the person has two consecutive tours h-w-h it will not be
 * allowed to take car to work go back home by pt go with pt again to work and
 * take a car back home. The agent will be forced to being the car back home in
 * the first tour. This was done in order to have easier implementation. If this
 * has an effect on the results is not clear.
 * 
 * @author Milos Balac <milos.balac@ivt.baug.ethz.ch>
 * @author Sebastian Hörl <sebastian.hoerl@ivt.baug.ethz.ch>
 */
public class VehicleTripConstraint implements TripConstraint {
	private final static Logger logger = Logger.getLogger(VehicleTripConstraint.class);

	private final List<DiscreteModeChoiceTrip> plan;
	private final Collection<String> restrictedModes;
	private final Id<? extends BasicLocation> homeLocationId;
	private final boolean isAdvanced;

	public VehicleTripConstraint(List<DiscreteModeChoiceTrip> plan, Collection<String> restrictedModes,
			Id<? extends BasicLocation> homeLocationId, boolean isAdvanced) {
		this.homeLocationId = homeLocationId;
		this.restrictedModes = restrictedModes;
		this.isAdvanced = isAdvanced;
		this.plan = plan;
	}

	private Id<? extends BasicLocation> getCurrentVehicleLocationId(String mode, List<String> previousModes) {
		int currentVehicleIndex = previousModes.lastIndexOf(mode);
		Id<? extends BasicLocation> currentVehicleLocationId = homeLocationId;

		if (currentVehicleIndex > -1) {
			currentVehicleLocationId = LocationUtils
					.getLocationId(plan.get(currentVehicleIndex).getDestinationActivity());
		}

		return currentVehicleLocationId;
	}

	/**
	 * Checks if the agent will return to this location before going home
	 */
	private boolean willReturnBeforeHome(Id<? extends BasicLocation> currentLocationId, DiscreteModeChoiceTrip trip) {
		int currentIndex = plan.indexOf(trip);

		for (DiscreteModeChoiceTrip futureTrip : plan.subList(currentIndex, plan.size())) {
			Id<? extends BasicLocation> futureLinkId = LocationUtils.getLocationId(futureTrip.getDestinationActivity());

			if (futureLinkId.equals(currentLocationId)) {
				return true;
			}

			if (futureLinkId.equals(homeLocationId)) {
				return false;
			}
		}

		return false;
	}

	@Override
	public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
		if (restrictedModes.contains(mode)) {
			Id<? extends BasicLocation> currentVehicleLocationId = getCurrentVehicleLocationId(mode, previousModes);
			Id<? extends BasicLocation> currentDepartureLocationId = LocationUtils
					.getLocationId(trip.getOriginActivity());

			return currentDepartureLocationId.equals(currentVehicleLocationId);
		}

		if (isAdvanced) {
			for (String testMode : restrictedModes) {
				Id<? extends BasicLocation> currentVehicleLocationId = getCurrentVehicleLocationId(testMode,
						previousModes);
				Id<? extends BasicLocation> currentDepartureLocationId = LocationUtils
						.getLocationId(trip.getOriginActivity());
				boolean isVehiclePresent = currentDepartureLocationId.equals(currentVehicleLocationId);

				if (isVehiclePresent && !currentDepartureLocationId.equals(homeLocationId)
						&& !willReturnBeforeHome(currentDepartureLocationId, trip)) {
					// We enforce the constrained mode, because otherwise the vehicle cannot return
					// home
					return mode.equals(testMode);
				}
			}

			return true;
		}

		return false;

	}

	@Override
	public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate,
			List<TripCandidate> previousCandidates) {
		return true;
	}

	static public class Factory implements TripConstraintFactory {
		private Collection<String> restrictedModes;
		private boolean isAdvanced;
		private final HomeFinder homeFinder;

		public Factory(Collection<String> restrictedModes, boolean isAdvanced, HomeFinder homeFinder) {
			this.restrictedModes = restrictedModes;
			this.isAdvanced = isAdvanced;
			this.homeFinder = homeFinder;
		}

		@Override
		public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
				Collection<String> availableModes) {
			logger.warn("VehicleTripConstraint is not tested. Use at own risk!");

			return new VehicleTripConstraint(planTrips, restrictedModes, homeFinder.getHomeLocationId(planTrips),
					isAdvanced);
		}
	}
}
