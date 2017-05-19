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

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.network.algorithms.*;
import org.matsim.core.network.algorithms.NetworkExpandNode.TurnInfo;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.*;
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
class LinkToLinkRoutingModule
    implements RoutingModule
{
    private final NetworkRoutingModule networkRoutingModule;
    private final InvertedNetworkLeastCostPathCalculator invertedNetworkRouteAlgo;


    LinkToLinkRoutingModule(final String mode, final PopulationFactory populationFactory,
            Network network, LeastCostPathCalculatorFactory leastCostPathCalcFactory,
            TravelDisutilityFactory travelCostCalculatorFactory,
            LinkToLinkTravelTime l2ltravelTimes, NetworkTurnInfoBuilderI turnInfoBuilder)
    {
        Map<Id<Link>, List<TurnInfo>> allowedInLinkTurnInfoMap = turnInfoBuilder
                .createAllowedTurnInfos();
        Network invertedNetwork = new NetworkInverter(network, allowedInLinkTurnInfoMap)
                .getInvertedNetwork();

        // convert l2ltravelTimes into something that can be used by the inverted network router:
        TravelTimesInvertedNetworkProxy travelTimesProxy = new TravelTimesInvertedNetworkProxy(
                network, l2ltravelTimes);
        // (method that takes a getLinkTravelTime( link , ...) with a link from the inverted network, converts it into links on the 
        // original network, and looks up the link2link tttime in the l2ltravelTimes data structure)

        TravelDisutility travelCost = travelCostCalculatorFactory
                .createTravelDisutility(travelTimesProxy);

        LeastCostPathCalculator routeAlgo = leastCostPathCalcFactory
                .createPathCalculator(invertedNetwork, travelCost, travelTimesProxy);

        invertedNetworkRouteAlgo = new InvertedNetworkLeastCostPathCalculator(network,
                invertedNetwork, routeAlgo);

        networkRoutingModule = new NetworkRoutingModule(mode, populationFactory, network,
                invertedNetworkRouteAlgo);
    }


    @Override
    public List<? extends PlanElement> calcRoute(final Facility<?> fromFacility,
            final Facility<?> toFacility, final double departureTime, final Person person)
    {
        invertedNetworkRouteAlgo.initBeforeCalcRoute(fromFacility, toFacility);
        return networkRoutingModule.calcRoute(fromFacility, toFacility, departureTime, person);
    }


    private static class InvertedNetworkLeastCostPathCalculator
        implements LeastCostPathCalculator
    {
        private final Network network;
        private final Network invertedNetwork;
        private final LeastCostPathCalculator delegate;

        private Id<Link> fromLinkId;
        private Id<Link> toLinkId;


        private InvertedNetworkLeastCostPathCalculator(Network network, Network invertedNetwork,
                LeastCostPathCalculator delegate)
        {
            this.network = network;
            this.invertedNetwork = invertedNetwork;
            this.delegate = delegate;
        }


        private void initBeforeCalcRoute(Facility<?> fromFacility, Facility<?> toFacility)
        {
            fromLinkId = fromFacility.getLinkId();
            toLinkId = toFacility.getLinkId();
        }


        public Path calcLeastCostPath(Node fromNode, Node toNode, double starttime, Person person,
                Vehicle vehicle)
        {
            //ignore fromNode and toNode
            Node fromInvNode = this.invertedNetwork.getNodes()
                    .get(Id.create(fromLinkId, Node.class));
            Node toInvNode = this.invertedNetwork.getNodes().get(Id.create(toLinkId, Node.class));

            Path invPath = delegate.calcLeastCostPath(fromInvNode, toInvNode, starttime, person,
                    vehicle);
            if (invPath == null) {
                throw new RuntimeException("No route found on inverted network from link "
                        + fromLinkId + " to link " + toLinkId + ".");
            }

            return invertPath(invPath);
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
//            nodes.add(links.get(0).getFromNode());
            /* use the first link of the inverted path instead of the first node of the just created link list. also works for invLinkCount 1. theresa, jan'17 */
            nodes.add(network.getNodes().get(Id.create(invPath.links.get(0).getId(), Node.class)));
            for (Link l : links) {
                nodes.add(l.getToNode());
            }

            return new Path(nodes, links, invPath.travelTime, invPath.travelCost);
        }
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


    @Override
    public StageActivityTypes getStageActivityTypes()
    {
        return networkRoutingModule.getStageActivityTypes();
    }
}
