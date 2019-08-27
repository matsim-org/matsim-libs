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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkInverter;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilderI;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;


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
class LinkToLinkRoutingModule implements RoutingModule
{
    private final Network invertedNetwork;
    private final Network network;
    private final LeastCostPathCalculator leastCostPathCalculator;
    private final PopulationFactory populationFactory;
    private final String mode;

    LinkToLinkRoutingModule(final String mode, final PopulationFactory populationFactory,
            Network network, LeastCostPathCalculatorFactory leastCostPathCalcFactory,
            TravelDisutilityFactory travelCostCalculatorFactory,
            LinkToLinkTravelTime l2ltravelTimes, NetworkTurnInfoBuilderI turnInfoBuilder)
    {
    	this.network = network;
    	this.populationFactory = populationFactory;
    	this.mode = mode;
    	
        invertedNetwork = new NetworkInverter(network, turnInfoBuilder.createAllowedTurnInfos()).getInvertedNetwork();

        // convert l2ltravelTimes into something that can be used by the inverted network router:
        TravelTimesInvertedNetworkProxy invertedTravelTimes = new TravelTimesInvertedNetworkProxy(network, l2ltravelTimes);
        // (method that takes a getLinkTravelTime( link , ...) with a link from the inverted network, converts it into links on the 
        // original network, and looks up the link2link tttime in the l2ltravelTimes data structure)

        TravelDisutility travelCost = travelCostCalculatorFactory.createTravelDisutility(invertedTravelTimes);

        leastCostPathCalculator = leastCostPathCalcFactory.createPathCalculator(invertedNetwork, travelCost, invertedTravelTimes);
    }

    @Override
    public List<? extends PlanElement> calcRoute( final Facility fromFacility,
								  final Facility toFacility, final double departureTime, final Person person)
    {	      
        Leg newLeg = this.populationFactory.createLeg( this.mode );
		
		Gbl.assertNotNull(fromFacility);
		Gbl.assertNotNull(toFacility);
		
		if (!toFacility.getLinkId().equals(fromFacility.getLinkId())) {
		    // (a "true" route)	        
		    Node fromInvNode = this.invertedNetwork.getNodes()
		            .get(Id.create(fromFacility.getLinkId(), Node.class));
		    Node toInvNode = this.invertedNetwork.getNodes().get(Id.create(toFacility.getLinkId(), Node.class));
		
		    Path invPath = leastCostPathCalculator.calcLeastCostPath(fromInvNode, toInvNode, departureTime, person, null);
		    if (invPath == null) {
		        throw new RuntimeException("No route found on inverted network from link "
		                + fromFacility.getLinkId() + " to link " + toFacility.getLinkId() + ".");
		    }		
		    Path path = invertPath(invPath);
		    
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
    
    private Path invertPath(Path invPath)
    {
        int invLinkCount = invPath.links.size();//==> normal node count

        //path search is called only if fromLinkId != toLinkId
        //see: org.matsim.core.router.NetworkRoutingModule.routeLeg()
        //implies: fromInvNode != toInvNode
        if (invLinkCount == 0) {
            throw new RuntimeException(
                    "The path in the inverted network should consist of at least one link.");
        }

        List<Link> links = new ArrayList<>(invLinkCount - 1);
        for (int i = 1; i < invLinkCount; i++) {
            Id<Link> linkId = Id.create(invPath.nodes.get(i).getId(), Link.class);
            links.add(network.getLinks().get(linkId));
        }

        List<Node> nodes = new ArrayList<>(invLinkCount);
//        nodes.add(links.get(0).getFromNode());
        /* use the first link of the inverted path instead of the first node of the just created link list. also works for invLinkCount 1. theresa, jan'17 */
        nodes.add(network.getNodes().get(Id.create(invPath.links.get(0).getId(), Node.class)));
        for (Link l : links) {
            nodes.add(l.getToNode());
        }

        return new Path(nodes, links, invPath.travelTime, invPath.travelCost);
    }
    

    private static class TravelTimesInvertedNetworkProxy
        implements TravelTime
    {
        private Network network;
        private LinkToLinkTravelTime linkToLinkTravelTime;


        private TravelTimesInvertedNetworkProxy(Network network, LinkToLinkTravelTime l2ltt)
        {
            this.linkToLinkTravelTime = l2ltt;
            this.network = network;
        }


        /**
         * In this case the link given as parameter is a link from the inverted network.
         * 
         * @see org.matsim.core.router.util.TravelTime#getLinkTravelTime(Link, double, Person,
         *      Vehicle)
         */
        @Override
        public double getLinkTravelTime(Link invLink, double time, Person person, Vehicle vehicle)
        {
            Link fromLink = network.getLinks()
                    .get(Id.create(invLink.getFromNode().getId(), Link.class));
            Link toLink = network.getLinks()
                    .get(Id.create(invLink.getToNode().getId(), Link.class));
            return linkToLinkTravelTime.getLinkToLinkTravelTime(fromLink, toLink, time);
        }
    }

}
