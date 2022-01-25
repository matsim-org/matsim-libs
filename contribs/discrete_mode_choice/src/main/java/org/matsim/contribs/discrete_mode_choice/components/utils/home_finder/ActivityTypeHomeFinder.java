package org.matsim.contribs.discrete_mode_choice.components.utils.home_finder;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.contribs.discrete_mode_choice.components.utils.LocationUtils;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/**
 * This HomeFinder implementation searches for a configurable activity type in
 * an agent's plan. As soon as the activity type is found, the respective
 * location is return as the "home" location of the agent. Default is activity
 * type "home".
 * 
 * @author sebhoerl
 */
public class ActivityTypeHomeFinder implements HomeFinder {
	private final Collection<String> activityTypes;
	private final String singleActivityType;

	public ActivityTypeHomeFinder(Collection<String> activityTypes) {
		this.activityTypes = activityTypes;
		this.singleActivityType = activityTypes.size() == 1 ? activityTypes.iterator().next() : null;
	}

	@Override
	public Id<? extends BasicLocation> getHomeLocationId(List<DiscreteModeChoiceTrip> trips) {
		for (DiscreteModeChoiceTrip trip : trips) {
			if (singleActivityType == null) {
				if (activityTypes.contains(trip.getOriginActivity().getType())) {
					return LocationUtils.getLocationId(trip.getOriginActivity());
				}

				if (activityTypes.contains(trip.getDestinationActivity().getType())) {
					return LocationUtils.getLocationId(trip.getDestinationActivity());
				}
			} else {
				if (trip.getOriginActivity().getType().equals(singleActivityType)) {
					return LocationUtils.getLocationId(trip.getOriginActivity());
				}

				if (trip.getDestinationActivity().getType().equals(singleActivityType)) {
					return LocationUtils.getLocationId(trip.getDestinationActivity());
				}
			}
		}

		return null;
	}
}
