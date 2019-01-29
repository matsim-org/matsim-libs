/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingsearch.manager;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author  jbischoff
 *
 */
public class WalkLegFactory {
	final Network network;
	final double walkspeed;
	final double beelinedistancefactor;
	
	@Inject
	public WalkLegFactory(Network network, Config config) {
		this.network = network;
		this.beelinedistancefactor = config.plansCalcRoute().getBeelineDistanceFactors().get("walk");
		this.walkspeed = config.plansCalcRoute().getTeleportedModeSpeeds().get("walk");
		
	}

	public Leg createWalkLeg(Id<Link> from, Id<Link> to, double startTime, String walkMode){
		Leg leg = PopulationUtils.createLeg(walkMode);
		double walkDistance = CoordUtils.calcEuclideanDistance(network.getLinks().get(from).getCoord(), network.getLinks().get(to).getCoord())*beelinedistancefactor;
		double walkTime = walkDistance / walkspeed;
		Route route = RouteUtils.createGenericRouteImpl(from, to);
		route.setDistance(walkDistance);
		route.setTravelTime(walkTime);
		leg.setRoute(route);
		leg.setDepartureTime(startTime);
		return leg;
	}
	
	
}
