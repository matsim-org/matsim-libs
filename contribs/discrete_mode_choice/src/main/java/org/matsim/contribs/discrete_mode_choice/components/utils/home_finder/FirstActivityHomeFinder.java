package org.matsim.contribs.discrete_mode_choice.components.utils.home_finder;

import java.util.List;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.contribs.discrete_mode_choice.components.utils.LocationUtils;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/**
 * This implementation of HomeFinder takes the first activity in an agent's plan
 * and return the location as the "home" location of the agent.
 * 
 * @author sebhoerl
 */
public class FirstActivityHomeFinder implements HomeFinder {
	@Override
	public Id<? extends BasicLocation> getHomeLocationId(List<DiscreteModeChoiceTrip> trips) {
		if (trips.size() > 0) {
			return LocationUtils.getLocationId(trips.get(0).getOriginActivity());
		} else {
			return null;
		}
	}
}
