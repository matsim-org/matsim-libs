/* *********************************************************************** *
 * project: org.matsim.*
 * DataContainerProvider.java
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

package org.matsim.core.trafficmonitoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public interface DataContainerProvider {
	
	/*
	 * This method is called from the EventHandler part of the TravelTimeCalculator. 
	 */
	/*package*/ TravelTimeData getTravelTimeData(final Id<Link> linkId, final boolean createIfMissing);
	
	/*
	 * This method is called from the TravelTime part of the TravelTimeCalculator.
	 */
	/*package*/ TravelTimeData getTravelTimeData(final Link link, final boolean createIfMissing);
}
