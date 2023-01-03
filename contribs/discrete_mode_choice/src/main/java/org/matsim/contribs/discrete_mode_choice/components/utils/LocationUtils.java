package org.matsim.contribs.discrete_mode_choice.components.utils;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.facilities.ActivityFacility;

/**
 * Some utilities for location handling.
 * 
 * @author sebhoerl
 */
public final class LocationUtils {
	private LocationUtils() {

	}

	/**
	 * Returns the facility id of an activity, or the link if no facility is set. If
	 * none is given, an exception is raised.
	 */
	public static Id<? extends BasicLocation> getLocationId(Activity activity) {
		Id<ActivityFacility> facilityId = activity.getFacilityId();

		if (facilityId != null) {
			return facilityId;
		}

		Id<Link> linkId = activity.getLinkId();

		if (linkId != null) {
			return linkId;
		}

		throw new IllegalStateException();
	}
}
