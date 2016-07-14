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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator.DataContainer;

public class MapBasedDataContainerProvider implements DataContainerProvider {

	private final Map<Id<Link>, DataContainer> linkData;
	private final TravelTimeDataFactory ttDataFactory;
	
	public MapBasedDataContainerProvider(Map<Id<Link>, DataContainer> linkData, TravelTimeDataFactory ttDataFactory) {
		this.linkData = linkData;
		this.ttDataFactory = ttDataFactory;
	}
	
	@Override 
	public DataContainer getTravelTimeData(final Id<Link> linkId, final boolean createIfMissing) {
		DataContainer data = this.linkData.get(linkId);
		if ((null == data) && createIfMissing) {
			data = new DataContainer(this.ttDataFactory.createTravelTimeData(linkId));
			this.linkData.put(linkId, data);
		}
		return data;
	}
		
	@Override
	public DataContainer getTravelTimeData(Link link, boolean createIfMissing) {
		return this.getTravelTimeData(link.getId(), createIfMissing);
	}
	
}