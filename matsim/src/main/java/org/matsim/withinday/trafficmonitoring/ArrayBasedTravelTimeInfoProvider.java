/* *********************************************************************** *
 * project: org.matsim.*
 * ArrayBasedTravelTimeInfoProvider.java
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
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.priorityqueue.HasIndex;
import org.matsim.withinday.trafficmonitoring.WithinDayTravelTime.TravelTimeInfo;

public class ArrayBasedTravelTimeInfoProvider implements TravelTimeInfoProvider {

	private final TravelTimeInfo[] arrayLinkData;
	private final TravelTimeInfoProvider delegate;
	
	public ArrayBasedTravelTimeInfoProvider(Map<Id<Link>, TravelTimeInfo> linkData, Network network) {
		this.delegate = new MapBasedTravelTimeInfoProvider(linkData);
		this.arrayLinkData = new TravelTimeInfo[linkData.size()];
	}
	
	/*
	 * This method is called from the EventHandler part of the WithinDayTravelTime.
	 * There, only link ids are available. We cannot optimize this. 
	 */
	@Override
	public TravelTimeInfo getTravelTimeInfo(final Id<Link> linkId) {
		return this.delegate.getTravelTimeInfo(linkId);
	}
	
	/*
	 * This method is called from the TravelTime part of the WithinDayTravelTime.
	 * There, link are available. we can optimize this by using an array instead of a map.
	 */
	@Override
	public TravelTimeInfo getTravelTimeInfo(Link link) {
		if (link instanceof HasIndex) {
			int index = ((HasIndex) link).getArrayIndex();
			TravelTimeInfo data = this.arrayLinkData[index];
			if (data == null) {
				data = this.delegate.getTravelTimeInfo(link);
				this.arrayLinkData[index] = data;
			}
			return data;
		} else {
			return this.delegate.getTravelTimeInfo(link);
		}
	}
	
}