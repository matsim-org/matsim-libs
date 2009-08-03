/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimesInvertedNetProxy
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

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;


/**
 * Proxy for a LinkToLinkTravelTime instance to make it work with the 
 * LeastCostPathCalculator working on an inverted network.
 * @author dgrether
 *
 */
public class TravelTimesInvertedNetProxy implements TravelTime {

	private NetworkLayer originalNetwork;
	
	private LinkToLinkTravelTime linkToLinkTravelTime;

	public TravelTimesInvertedNetProxy(NetworkLayer originalNet, LinkToLinkTravelTime l2ltt){
		this.linkToLinkTravelTime = l2ltt;
		this.originalNetwork = originalNet;
	}
	
	/**
	 * @see org.matsim.core.router.util.TravelTime#getLinkTravelTime(Link, double)
	 */
	public double getLinkTravelTime(Link link, double time) {
		LinkImpl fromLink = this.originalNetwork.getLink(link.getFromNode().getId());
		LinkImpl toLink = this.originalNetwork.getLink(link.getToNode().getId());
		return this.linkToLinkTravelTime.getLinkToLinkTravelTime(fromLink, toLink, time);
	}

}
