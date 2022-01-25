package org.matsim.contribs.discrete_mode_choice.components.utils.home_finder;

import java.util.List;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/**
 * This interface is used by the vehicle constraints to find the home location
 * in an agent's plan.
 * 
 * @author sebhoerl
 */
public interface HomeFinder {
	Id<? extends BasicLocation> getHomeLocationId(List<DiscreteModeChoiceTrip> trips);
}
