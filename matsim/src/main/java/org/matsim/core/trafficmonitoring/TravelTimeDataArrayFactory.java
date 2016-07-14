/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeDataArrayFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

public class TravelTimeDataArrayFactory implements TravelTimeDataFactory {

	private final Network network;
	private final int numSlots;
	
	public TravelTimeDataArrayFactory(final Network network, final int numSlots) {
		this.network = network;
		this.numSlots = numSlots;
	}
	
	@Override
	public TravelTimeData createTravelTimeData(Id<Link> linkId) {
		return new TravelTimeDataArray(this.network.getLinks().get(linkId), this.numSlots);
	}

}
