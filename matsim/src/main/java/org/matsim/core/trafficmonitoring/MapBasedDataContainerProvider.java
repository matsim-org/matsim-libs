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

package org.matsim.core.trafficmonitoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.Map;

class MapBasedDataContainerProvider implements DataContainerProvider {

	private final Map<Id<Link>, TravelTimeData> linkData;
	private final TravelTimeDataFactory ttDataFactory;
	
	public MapBasedDataContainerProvider(Map<Id<Link>, TravelTimeData> linkData, TravelTimeDataFactory ttDataFactory) {
		this.linkData = linkData;
		this.ttDataFactory = ttDataFactory;
	}
	
	@Override 
	public TravelTimeData getTravelTimeData(final Id<Link> linkId, final boolean createIfMissing) {
		TravelTimeData data = this.linkData.get(linkId);
		if ((null == data) && createIfMissing) {
			data = this.ttDataFactory.createTravelTimeData(linkId) ;
			this.linkData.put(linkId, data);
		}
		return data;
	}
		
	@Override
	public TravelTimeData getTravelTimeData(Link link, boolean createIfMissing) {
		return this.getTravelTimeData(link.getId(), createIfMissing);
	}
	
}
