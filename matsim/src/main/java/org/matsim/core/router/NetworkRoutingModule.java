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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.facilities.Facility;

/**
 * This wraps a "computer science" {@link LeastCostPathCalculator}, which routes from a node to another node, into something that
 * routes from a {@link Facility} to another {@link Facility}, as we need in MATSim.
 * 
 * @author thibautd
 */
public final class NetworkRoutingModule implements RoutingModule {
	// I think it makes sense to NOT add the bushwhacking mode directly into here ...
	// ... since it makes sense be able to to route from facility.getLinkId() to facility.getLinkId(). kai, dec'15

	private final String mode;
	private final PopulationFactory populationFactory;

	private final Network network;
	private final LeastCostPathCalculator routeAlgo;


	 public NetworkRoutingModule(
			final String mode,
			final PopulationFactory populationFactory,
			final Network network,
			final LeastCostPathCalculator routeAlgo) {
		 Gbl.assertNotNull(network);
//		 Gbl.assertIf( network.getLinks().size()>0 ) ; // otherwise network for mode probably not defined
		 // makes many tests fail.  
		 this.network = network;
		 this.routeAlgo = routeAlgo;
		 this.mode = mode;
		 this.populationFactory = populationFactory;
	}

	@Override
	public List<? extends PlanElement> calcRoute(final Facility fromFacility, final Facility toFacility, final double departureTime,
			final Person person) {		
		Leg newLeg = this.populationFactory.createLeg( this.mode );

		Gbl.assertNotNull(fromFacility);
		Gbl.assertNotNull(toFacility);

		Link fromLink = this.network.getLinks().get(fromFacility.getLinkId());
		if ( fromLink==null ) {
			Gbl.assertNotNull( fromFacility.getCoord() ) ;
			fromLink = NetworkUtils.getNearestLink( network, fromFacility.getCoord()) ;
		}
		Link toLink = this.network.getLinks().get(toFacility.getLinkId());
		if ( toLink==null ) {
			Gbl.assertNotNull( toFacility.getCoord() ) ;
			toLink = NetworkUtils.getNearestLink(network, toFacility.getCoord());
		}
		Gbl.assertNotNull(fromLink);
		Gbl.assertNotNull(toLink);
		
		if (toLink != fromLink) {
			// (a "true" route)
			Node startNode = fromLink.getToNode(); // start at the end of the "current" link
			Node endNode = toLink.getFromNode(); // the target is the start of the link
			Path path = this.routeAlgo.calcLeastCostPath(startNode, endNode, departureTime, person, null);
			if (path == null)
				throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
			NetworkRoute route = this.populationFactory.getRouteFactories().createRoute(NetworkRoute.class, fromLink.getId(), toLink.getId());
			route.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
			route.setTravelTime(path.travelTime);
			route.setTravelCost(path.travelCost);
			route.setDistance(RouteUtils.calcDistance(route, 1.0, 1.0, this.network));
			newLeg.setRoute(route);
			newLeg.setTravelTime(path.travelTime);
		} else {
			// create an empty route == staying on place if toLink == endLink
			// note that we still do a route: someone may drive from one location to another on the link. kai, dec'15
			NetworkRoute route = this.populationFactory.getRouteFactories().createRoute(NetworkRoute.class, fromLink.getId(), toLink.getId());
			route.setTravelTime(0);
			route.setDistance(0.0);
			newLeg.setRoute(route);
			newLeg.setTravelTime(0);
		}
		newLeg.setDepartureTime(departureTime);

		return Arrays.asList( newLeg );
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EmptyStageActivityTypes.INSTANCE;
	}

	@Override
	public String toString() {
		return "[NetworkRoutingModule: mode="+this.mode+"]";
	}

}
