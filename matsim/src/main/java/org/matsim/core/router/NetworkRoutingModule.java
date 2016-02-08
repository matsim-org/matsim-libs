/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package org.matsim.core.router;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.facilities.Facility;

/**
 * @thibautd
 */
public class NetworkRoutingModule implements RoutingModule {

	private final String mode;
	private final PopulationFactory populationFactory;

	private final Network network;
	private final ModeRouteFactory routeFactory;
	private final LeastCostPathCalculator routeAlgo;


	 public NetworkRoutingModule(
			final String mode,
			final PopulationFactory populationFactory,
			final Network network,
			final LeastCostPathCalculator routeAlgo,
			final ModeRouteFactory routeFactory) {
		this.network = network;
		this.routeAlgo = routeAlgo;
		this.routeFactory = routeFactory;
		this.mode = mode;
		this.populationFactory = populationFactory;
	}

	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		Leg newLeg = populationFactory.createLeg( mode );
		newLeg.setDepartureTime( departureTime );

		double travTime = routeLeg(
				person,
				newLeg,
				new FacilityWrapperActivity( fromFacility ),
				new FacilityWrapperActivity( toFacility ),
				departureTime);

		// otherwise, information may be lost
		newLeg.setTravelTime( travTime );

		return Arrays.asList( newLeg );
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EmptyStageActivityTypes.INSTANCE;
	}

	@Override
	public String toString() {
		return "[NetworkRoutingModule: mode="+mode+"]";
	}

	/*package (Tests)*/ double routeLeg(Person person, Leg leg, Activity fromAct, Activity toAct, double depTime) {
		double travTime = 0;
		Link fromLink = this.network.getLinks().get(fromAct.getLinkId());
		Link toLink = this.network.getLinks().get(toAct.getLinkId());

		/* Remove this and next three lines once debugged. */
		if(fromLink == null || toLink == null){
			Logger.getLogger(NetworkRoutingModule.class).error("  ==>  null from/to link for person " + person.getId().toString());
		}
		if (fromLink == null) throw new RuntimeException("fromLink "+fromAct.getLinkId()+" missing.");
		if (toLink == null) throw new RuntimeException("toLink "+toAct.getLinkId()+" missing.");

		Node startNode = fromLink.getToNode();	// start at the end of the "current" link
		Node endNode = toLink.getFromNode(); // the target is the start of the link

//		CarRoute route = null;
		Path path = null;
		if (toLink != fromLink) {
			// (a "true" route)
			path = this.routeAlgo.calcLeastCostPath(startNode, endNode, depTime, person, null);
			if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
			NetworkRoute route = this.routeFactory.createRoute(NetworkRoute.class, fromLink.getId(), toLink.getId());
			route.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
			route.setTravelTime((int) path.travelTime);
			route.setTravelCost(path.travelCost);
			route.setDistance(RouteUtils.calcDistance(route, this.network));
			leg.setRoute(route);
			travTime = (int) path.travelTime;
		} else {
			// create an empty route == staying on place if toLink == endLink
			// note that we still do a route: someone may drive from one location to another on the link. kai, dec'15
			NetworkRoute route = this.routeFactory.createRoute(NetworkRoute.class, fromLink.getId(), toLink.getId());
			route.setTravelTime(0);
			route.setDistance(0.0);
			leg.setRoute(route);
			travTime = 0;
		}

		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		if ( leg instanceof LegImpl ) {
			((LegImpl) leg).setArrivalTime(depTime + travTime); 
			// (not in interface!)
		}
		return travTime;
	}

}
