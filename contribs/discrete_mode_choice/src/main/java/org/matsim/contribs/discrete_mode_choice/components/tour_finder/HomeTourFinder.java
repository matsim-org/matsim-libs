package org.matsim.contribs.discrete_mode_choice.components.tour_finder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contribs.discrete_mode_choice.components.utils.LocationUtils;
import org.matsim.contribs.discrete_mode_choice.components.utils.home_finder.HomeFinder;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/**
 * This TourFinder makes use of the HomeFinder that is defined in configuration.
 * Tours will start and end at activities that are identified as home.
 * 
 * @author shoerl
 */
public class HomeTourFinder extends AbstractTourFinder {
	private final HomeFinder homeFinder;

	public HomeTourFinder(HomeFinder homeFinder) {
		this.homeFinder = homeFinder;
	}

	@Override
	protected Set<Activity> findActivities(List<DiscreteModeChoiceTrip> trips) {
		Set<Activity> relevantActivities = new HashSet<>();

		Id<? extends BasicLocation> homeLocationId = homeFinder.getHomeLocationId(trips);

		for (DiscreteModeChoiceTrip trip : trips) {
			if (LocationUtils.getLocationId(trip.getDestinationActivity()).equals(homeLocationId)) {
				relevantActivities.add(trip.getDestinationActivity());
			}

			if (LocationUtils.getLocationId(trip.getDestinationActivity()).equals(homeLocationId)) {
				relevantActivities.add(trip.getDestinationActivity());
			}
		}

		return relevantActivities;
	}
}
