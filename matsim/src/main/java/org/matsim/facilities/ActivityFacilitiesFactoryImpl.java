/* *********************************************************************** *
 * project: matsim
 * ActivityFacilitiesFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.facilities;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * @author nagel
 *
 */
public class ActivityFacilitiesFactoryImpl implements ActivityFacilitiesFactory {

	@Override
	public ActivityFacility createActivityFacility(Id<ActivityFacility> id, Coord coord) {
		return new ActivityFacilityImpl(id, coord, null);
	}
	
	@Override
	public ActivityFacility createActivityFacility(Id<ActivityFacility> id, Id<Link> linkId) {
		return new ActivityFacilityImpl(id, null, linkId);
	}
	
	@Override
	public ActivityFacility createActivityFacility(Id<ActivityFacility> id, Coord coord, Id<Link> linkId) {
		return new ActivityFacilityImpl(id, coord, linkId);
	}

	@Override
	public ActivityOption createActivityOption(String type) {
		return new ActivityOptionImpl(type);
	}

}
