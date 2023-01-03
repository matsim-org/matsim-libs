/* *********************************************************************** *
 * project: org.matsim.*
 * InvertedNetworkLegRouter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
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

import java.util.Arrays;
import java.util.List;


/**
 * This leg router takes travel times needed for turning moves into account. This is done by a
 * routing on an inverted network, i.e. the links of the street networks are converted to nodes and
 * for each turning move a link is inserted. This LegRouter can only be used if the
 * enableLinkToLinkRouting parameter in the controler config module is set and AStarLandmarks
 * routing is not enabled.
 * 
 * @author dgrether
 * @author michalm
 */
class LinkToLinkRoutingModule implements RoutingModule {
    private final Network network;
    private final Network invertedNetwork;
    private final LeastCostPathCalculator leastCostPathCalculator;
    private final PopulationFactory populationFactory;
    private final String mode;

    LinkToLinkRoutingModule(final String mode, final PopulationFactory populationFactory,
            Network network, Network invertedNetwork, InvertedLeastPathCalculator leastCostPathCalc) {
    	this.network = network;
    	this.invertedNetwork = invertedNetwork;
    	this.populationFactory = populationFactory;
    	this.mode = mode;
        this.leastCostPathCalculator = leastCostPathCalc;
    }

    @Override
    public List<? extends PlanElement> calcRoute(RoutingRequest request) {
		final Facility fromFacility = request.getFromFacility();
		final Facility toFacility = request.getToFacility();
		final double departureTime = request.getDepartureTime();
		final Person person = request.getPerson();
		
        Leg newLeg = this.populationFactory.createLeg( this.mode );
		
		Gbl.assertNotNull(fromFacility);
		Gbl.assertNotNull(toFacility);
		
		if (!toFacility.getLinkId().equals(fromFacility.getLinkId())) {
		    // (a "true" route)	        
		    Node fromInvNode = this.invertedNetwork.getNodes()
		            .get(Id.create(fromFacility.getLinkId(), Node.class));
		    Node toInvNode = this.invertedNetwork.getNodes().get(Id.create(toFacility.getLinkId(), Node.class));
		
		    Path path = leastCostPathCalculator.calcLeastCostPath(fromInvNode, toInvNode, departureTime, person, null);
		    if (path == null) {
		        throw new RuntimeException("No route found on inverted network from link "
		                + fromFacility.getLinkId() + " to link " + toFacility.getLinkId() + ".");
		    }		

			NetworkRoute route = this.populationFactory.getRouteFactories().createRoute(NetworkRoute.class, fromFacility.getLinkId(), toFacility.getLinkId());
			route.setLinkIds(fromFacility.getLinkId(), NetworkUtils.getLinkIds(path.links), toFacility.getLinkId());
			route.setTravelTime(path.travelTime);
			route.setTravelCost(path.travelCost);
			route.setDistance(RouteUtils.calcDistance(route, 1.0, 1.0, this.network));
			newLeg.setRoute(route);
			newLeg.setTravelTime(path.travelTime);
		} else {
			// create an empty route == staying on place if toLink == endLink
			// note that we still do a route: someone may drive from one location to another on the link. kai, dec'15
			NetworkRoute route = this.populationFactory.getRouteFactories().createRoute(NetworkRoute.class, fromFacility.getLinkId(), toFacility.getLinkId());
			route.setTravelTime(0);
			route.setDistance(0.0);
			newLeg.setRoute(route);
			newLeg.setTravelTime(0);
		}		
		newLeg.setDepartureTime(departureTime);
		return Arrays.asList( newLeg );
    }


}
