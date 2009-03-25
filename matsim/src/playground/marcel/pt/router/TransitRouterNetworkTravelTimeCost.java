/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterNetworkCost.java
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

package playground.marcel.pt.router;

import org.matsim.core.api.network.Link;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;

import playground.marcel.pt.router.TransitRouterNetworkWrapper.LinkWrapper;

public class TransitRouterNetworkTravelTimeCost implements TravelCost, TravelTime {

	public double getLinkTravelCost(Link link, double time) {
		return getLinkTravelTime(link, time);
	}

	public double getLinkTravelTime(Link link, double time) {
		LinkWrapper wrapped = (LinkWrapper) link;
		return wrapped.link.toNode.stop.getDepartureDelay() - wrapped.link.fromNode.stop.getDepartureDelay(); // does not yet respect departure time
	}

}
