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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;


/**
 * Proxy for a LinkToLinkTravelTime instance to make it work with the 
 * LeastCostPathCalculator working on an inverted network.
 * @author dgrether
 * @see org.matsim.core.route.util.NetworkInverter
 *
 */
public class TravelTimesInvertedNetProxy implements PersonalizableTravelTime {

	private Network originalNetwork;
	
	private PersonalizableLinkToLinkTravelTime linkToLinkTravelTime;

	public TravelTimesInvertedNetProxy(Network originalNet, PersonalizableLinkToLinkTravelTime l2ltt){
		this.linkToLinkTravelTime = l2ltt;
		this.originalNetwork = originalNet;
	}
	
	/**
	 * In this case the link given as parameter is a link from the inverted network. 
	 * @see org.matsim.core.router.util.TravelTime#getLinkTravelTime(Link, double)
	 */
	@Override
	public double getLinkTravelTime(Link link, double time) {
		Link fromLink = this.originalNetwork.getLinks().get(link.getFromNode().getId());
		Link toLink = this.originalNetwork.getLinks().get(link.getToNode().getId());
		return this.linkToLinkTravelTime.getLinkToLinkTravelTime(fromLink, toLink, time);
	}

	@Override
	public void setPerson(Person person) {
		this.linkToLinkTravelTime.setPerson(person);
	}

}
