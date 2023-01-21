package org.matsim.contribs.discrete_mode_choice.components.constraints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.components.utils.IndexUtils;
import org.matsim.contribs.discrete_mode_choice.components.utils.LocationUtils;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTourConstraint;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraint;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraintFactory;

/**
 * This constraint filters out plans that cannot be produced by
 * SubtourModeChoice. It receives a list of constrained modes for which the
 * content depends on how SubtourModeChoice is used. If singleTripProbability ==
 * 0.0 (i.e. the old standard setting), all availabe modes should be set as
 * "constrained modes". This means that every sub-tour in the plan *must* be
 * covered by one and a single mode. If singleLegProbability was > 0.0, then
 * only the "chain based" modes should be set as constrained modes. In that case
 * only those tours cannot be interrupted by other modes.
 * 
 * @author sebhoerl
 *
 */
public class SubtourModeConstraint extends AbstractTourConstraint {
	private final Collection<String> constrainedModes;
	private final List<Id<? extends BasicLocation>> originLocations;
	private final List<Id<? extends BasicLocation>> destinationLocations;

	public SubtourModeConstraint(Collection<String> constrainedModes, List<Id<? extends BasicLocation>> originLocations,
			List<Id<? extends BasicLocation>> destinationLocations) {
		this.constrainedModes = constrainedModes;
		this.originLocations = originLocations;
		this.destinationLocations = destinationLocations;
	}

	@Override
	public boolean validateBeforeEstimation(List<DiscreteModeChoiceTrip> tour, List<String> modes,
			List<List<String>> previousModes) {
		int tourLocationOffset = IndexUtils.getFirstTripIndex(previousModes);

		for (int index = 0; index < modes.size(); index++) {
			// We loop over all trips
			Id<? extends BasicLocation> startLocationId = originLocations.get(index + tourLocationOffset);

			for (int offset = 0; offset + index < modes.size(); offset++) {
				// We loop over all following destinations

				if (destinationLocations.get(offset + index + tourLocationOffset).equals(startLocationId)) {
					// We found a destination that has the origin location. Now we need to check
					// that all modes in between are of the same type.
					String mode = modes.get(index + tourLocationOffset);

					for (int testIndex = index + 1; testIndex <= index + offset; testIndex++) {
						String testMode = modes.get(testIndex);

						if (!mode.equals(testMode)
								&& (constrainedModes.contains(testMode) || constrainedModes.contains(mode))) {
							return false;
						}
					}

					index += offset;
					break;
				}
			}
		}

		return true;
	}

	static public class Factory implements TourConstraintFactory {
		private final Collection<String> constrainedModes;

		public Factory(Collection<String> constrainedModes) {
			this.constrainedModes = constrainedModes;
		}

		@Override
		public TourConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> trips,
				Collection<String> availableModes) {
			List<Id<? extends BasicLocation>> originLocations = new ArrayList<>(trips.size());
			List<Id<? extends BasicLocation>> destinationLocations = new ArrayList<>(trips.size());

			for( DiscreteModeChoiceTrip trip : trips ){
				originLocations.add( LocationUtils.getLocationId( trip.getOriginActivity() ) );
				destinationLocations.add( LocationUtils.getLocationId( trip.getDestinationActivity() ) );
			}

			return new SubtourModeConstraint(constrainedModes, originLocations, destinationLocations);
		}
	}
}
