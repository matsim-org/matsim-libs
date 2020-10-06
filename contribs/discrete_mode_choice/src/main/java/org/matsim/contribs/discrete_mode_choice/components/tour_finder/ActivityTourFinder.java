package org.matsim.contribs.discrete_mode_choice.components.tour_finder;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/**
 * This TourFinder creates tours between activities of a certain type. For
 * instance, all trips between two home activities are considered as one trip.
 * 
 * @author sebhoerl
 */
public class ActivityTourFinder extends AbstractTourFinder {
	private final Collection<String> activityTypes;
	private final String singleActivityType;

	/**
	 * Defines which activity type is used to establish tours.
	 */
	public ActivityTourFinder(Collection<String> activityTypes) {
		this.activityTypes = activityTypes;
		this.singleActivityType = activityTypes.size() == 1 ? activityTypes.iterator().next() : null;
	}

	@Override
	protected Set<Activity> findActivities(List<DiscreteModeChoiceTrip> trips) {
		Set<Activity> relevantActivities = new HashSet<>();

		for (int index = 0; index < trips.size(); index++) {
			DiscreteModeChoiceTrip trip = trips.get(index);

			if (singleActivityType == null) {
				if (activityTypes.contains(trip.getOriginActivity().getType())) {
					relevantActivities.add(trip.getOriginActivity());
				}

				if (activityTypes.contains(trip.getDestinationActivity().getType())) {
					relevantActivities.add(trip.getDestinationActivity());
				}
			} else {
				if (trip.getOriginActivity().getType().equals(singleActivityType)) {
					relevantActivities.add(trip.getOriginActivity());
				}

				if (trip.getDestinationActivity().getType().equals(singleActivityType)) {
					relevantActivities.add(trip.getDestinationActivity());
				}
			}
		}

		return relevantActivities;
	}
}
