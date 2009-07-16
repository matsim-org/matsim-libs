/* *********************************************************************** *
 * project: org.matsim.*
 * TravelCostsInvertedNetProxy
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
package org.matsim.core.router.util;

import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;

/**
 * Proxy for a TravelCost instance to make it work with the 
 * LeastCostPathCalculator working on an inverted network.
 * 
 * @author dgrether
 *
 */
public class TravelCostsInvertedNetProxy implements TravelCost {

	private NetworkLayer originalNetwork;
	private TravelCost travelCosts;

	public TravelCostsInvertedNetProxy(NetworkLayer originalNetwork,
			TravelCost travelCosts) {
		this.originalNetwork = originalNetwork;
		this.travelCosts = travelCosts;
	}

	public double getLinkTravelCost(Link link, double time) {
		//as we have no turning move travel costs defined
		//the fromLink is sufficient to calculate travelCosts
		LinkImpl fromLink = this.originalNetwork.getLink(link.getFromNode().getId());
		return this.travelCosts.getLinkTravelCost(fromLink, time);
	}

}
