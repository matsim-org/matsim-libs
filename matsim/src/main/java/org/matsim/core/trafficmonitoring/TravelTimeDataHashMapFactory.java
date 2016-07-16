/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeDataHashMapFactory.java
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

public class TravelTimeDataHashMapFactory implements TravelTimeDataFactory {

	private final Network network;
	
	public TravelTimeDataHashMapFactory(final Network network) {
		this.network = network;
	}
	
	@Override
	public TravelTimeData createTravelTimeData(Id<Link> linkId) {
		return new TravelTimeDataHashMap(this.network.getLinks().get(linkId));
	}

}
