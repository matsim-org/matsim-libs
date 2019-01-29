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
import org.matsim.core.api.internal.MatsimFactory;

/**
 * @author nagel
 *
 */
public interface ActivityFacilitiesFactory extends MatsimFactory {

	public ActivityFacility createActivityFacility(Id<ActivityFacility> id, Coord coord);
	
	public ActivityFacility createActivityFacility(Id<ActivityFacility> id, Id<Link> linkId);
	
	public ActivityFacility createActivityFacility(Id<ActivityFacility> id, Coord coord, Id<Link> linkId);

	public ActivityOption createActivityOption(String type);

}
