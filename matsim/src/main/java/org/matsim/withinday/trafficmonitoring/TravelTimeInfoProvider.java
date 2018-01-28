/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeInfoProvider.java
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

package org.matsim.withinday.trafficmonitoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.withinday.trafficmonitoring.WithinDayTravelTime.TravelTimeInfo;

public interface TravelTimeInfoProvider {
	
	/*package*/ TravelTimeInfo getTravelTimeInfo(final Id<Link> linkId);
	
	/*package*/ TravelTimeInfo getTravelTimeInfo(final Link link);
	// needs to be available separately since sometimes calling
	// with the link argument is faster. kai, dec'17
}
