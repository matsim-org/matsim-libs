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
package org.matsim.contrib.signals.router;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;


/**
 * Proxy for a LinkToLinkTravelTime instance to make it work with the 
 * LeastCostPathCalculator working on an inverted network.
 * @author dgrether
 * @see NetworkInverter
 *
 */
class TravelTimesInvertedNetProxy implements TravelTime {

	private Network originalNetwork;
	
	private LinkToLinkTravelTime linkToLinkTravelTime;

	public TravelTimesInvertedNetProxy(Network originalNet, LinkToLinkTravelTime l2ltt){
		this.linkToLinkTravelTime = l2ltt;
		this.originalNetwork = originalNet;
	}
	
	/**
	 * In this case the link given as parameter is a link from the inverted network. 
	 * @see org.matsim.core.router.util.TravelTime#getLinkTravelTime(Link, double, Person, Vehicle)
	 */
	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		Link fromLink = this.originalNetwork.getLinks().get(Id.create(link.getFromNode().getId(), Link.class));
		Link toLink = this.originalNetwork.getLinks().get(Id.create(link.getToNode().getId(), Link.class));
		return this.linkToLinkTravelTime.getLinkToLinkTravelTime(fromLink, toLink, time);
	}

}
