/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.random;

import java.util.ArrayList;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.RandomParkingSearch;

public class RandomStreetParkingSearchExp1 extends RandomParkingSearch{
	
	double searchCircleDistance=100;
	double slope=1.0;
	
	public RandomStreetParkingSearchExp1(double maxDistance, Network network, String name) {
		super(maxDistance, network, name);
		this.parkingType="streetParking";
	}

	public double getMaxDistance(double searchTime){
		return searchCircleDistance+slope*searchTime;
	}
	
	@Override
	public void addRandomLinkToRoute(LinkNetworkRouteImpl route) {
		Random r = new Random();
		Link link = network.getLinks().get(route.getEndLinkId());

	//	Link nextLink = randomNextLink(link);
		ArrayList<Id> newRoute = new ArrayList<Id>();
		newRoute.addAll(route.getLinkIds());
		newRoute.add(link.getId());
	//	route.setLinkIds(route.getStartLinkId(), newRoute, nextLink.getId());
	//	route.setEndLinkId(nextLink.getId());
	}
}

