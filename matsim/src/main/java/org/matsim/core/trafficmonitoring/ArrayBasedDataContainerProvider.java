/* *********************************************************************** *
 * project: org.matsim.*
 * ArrayBasedDataContainerProvider.java
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
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.priorityqueue.HasIndex;

import java.util.Map;

/**
 *  Uses an array to store DataContainer object for the TravelTimeCalculator.
 *  It is tried to get a DataContainer's position in the array is taken from the
 *  the Link, which is possible if routing networks are used (there, link implement
 *  the HasIndex interface).
 *  
 *  If regular network are used, calls are forwarded to a MapBasedDataContainerProvider,
 *  which represents the lookup approach used so far.
 *  
 * @author cdobler
 */
class ArrayBasedDataContainerProvider implements DataContainerProvider {

	private final TravelTimeData[] arrayLinkData;
	private final DataContainerProvider delegate;
	
	public ArrayBasedDataContainerProvider(Map<Id<Link>, TravelTimeData> linkData, TravelTimeDataFactory ttDataFactory,
			Network network) {
		this.arrayLinkData = new TravelTimeData[network.getLinks().size()];
		this.delegate = new MapBasedDataContainerProvider(linkData, ttDataFactory);
	}
	
	/*
	 * This method is called from the EventHandler part of the TravelTimeCalculator.
	 * There, only link ids are available. We cannot optimize this. 
	 */
	@Override
	public TravelTimeData getTravelTimeData(final Id<Link> linkId, final boolean createIfMissing) {
		return this.delegate.getTravelTimeData(linkId, createIfMissing);
	}
	
	/*
	 * This method is called from the TravelTime part of the TravelTimeCalculator.
	 * There, links are available. we can optimize this by using an array instead of a map.
	 * 
	 *  Probably pre-initialize all DataContainers to avoid the null-check?
	 */
	@Override
	public TravelTimeData getTravelTimeData(Link link, boolean createIfMissing) {
		if (link instanceof HasIndex) {
			int index = ((HasIndex) link).getArrayIndex();
			TravelTimeData data = this.arrayLinkData[index];
			if (data == null) {
				data = this.delegate.getTravelTimeData(link, createIfMissing);
				this.arrayLinkData[index] = data;
			}
			return data;
		} else {
			return this.delegate.getTravelTimeData(link, createIfMissing);
		}
	}
	
}
