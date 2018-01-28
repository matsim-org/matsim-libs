/* *********************************************************************** *
 * project: org.matsim.*
 * MapBasedDataContainerProvider.java
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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.withinday.trafficmonitoring.WithinDayTravelTime.TravelTimeInfo;

public class MapBasedTravelTimeInfoProvider implements TravelTimeInfoProvider {

	private final Map<Id<Link>, TravelTimeInfo> linkData;
	
	public MapBasedTravelTimeInfoProvider(Map<Id<Link>, TravelTimeInfo> linkData) {
		this.linkData = linkData;
	}
	
	@Override 
	public TravelTimeInfo getTravelTimeInfo(final Id<Link> linkId) {
		TravelTimeInfo data = this.linkData.get(linkId);
		return data;
	}
		
	@Override
	public TravelTimeInfo getTravelTimeInfo(Link link) {
		Id<Link> linkId = link.getId();
		return this.getTravelTimeInfo(linkId);
	}
	
}