/* *********************************************************************** *
 * project: org.matsim.*
 * TranitRouter.java
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

package org.matsim.pt.router;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.InitialNode;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * Not thread-safe because MultiNodeDijkstra is not. Does not expect the TransitSchedule to change once constructed! michaz '13
 *
 * @author mrieser
 */
public class TransitRouterImpl extends AbstractTransitRouter implements TransitRouter {

	public TransitRouterImpl(final TransitRouterConfig trConfig, final TransitSchedule schedule) {
		super(trConfig, schedule) ;
	}

	public TransitRouterImpl(
			final TransitRouterConfig config,
			// even though it is not used now. This constructor is used at many places, so keeping it here.
			//However, replacing it with transitSchedule will help to get rid of TransitRouterNetwork from AbstractTransitRouter. Amit Oct'17
			final PreparedTransitSchedule preparedTransitSchedule,
			final TransitRouterNetwork routerNetwork,
			final TravelTime travelTime,
			final TransitTravelDisutility travelDisutility) {
		super( config, routerNetwork, travelTime, travelDisutility) ;
	}

	private final Map<Node, InitialNode> locateWrappedNearestTransitNodes(Person person, Coord coord, double departureTime) {
		Collection<TransitRouterNetwork.TransitRouterNetworkNode> nearestNodes = this.getTransitRouterNetwork().getNearestNodes(coord, this.getConfig().getSearchRadius());
		if (nearestNodes.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitRouterNetwork.TransitRouterNetworkNode nearestNode = this.getTransitRouterNetwork().getNearestNode(coord);
			if ( nearestNode != null ) { // transit schedule might be completely empty!
				double distance = CoordUtils.calcEuclideanDistance(coord, nearestNode.stop.getStopFacility().getCoord());
				nearestNodes = this.getTransitRouterNetwork().getNearestNodes(coord, distance + this.getConfig().getExtensionRadius());
			}
		}
		Map<Node, InitialNode> wrappedNearestNodes = new LinkedHashMap<>();
		for (TransitRouterNetwork.TransitRouterNetworkNode node : nearestNodes) {
			Coord toCoord = node.stop.getStopFacility().getCoord();
			double initialTime = getWalkTime(person, coord, toCoord);
			double initialCost = getWalkDisutility(person, coord, toCoord);
			wrappedNearestNodes.put(node, new InitialNode(initialCost, initialTime + departureTime));
		}
		return wrappedNearestNodes;
	}

	@Override
	public List<Leg> calcRoute(final Facility<?> fromFacility, final Facility<?> toFacility, final double departureTime, final Person person) {
		// find possible start stops
		Map<Node, InitialNode> wrappedFromNodes = this.locateWrappedNearestTransitNodes(person, fromFacility.getCoord(), departureTime);
		// find possible end stops
		Map<Node, InitialNode> wrappedToNodes = this.locateWrappedNearestTransitNodes(person, toFacility.getCoord(), departureTime);
		
		double pathCost = Double.POSITIVE_INFINITY ;
		TransitPassengerRoute transitPassengerRoute = null;

		if ( wrappedFromNodes != null && wrappedToNodes != null ) {

			TransitLeastCostPathTree tree = new TransitLeastCostPathTree(getTransitRouterNetwork(), getTravelDisutility(), getTravelTime(),
					wrappedFromNodes, wrappedToNodes, person);
			// yyyyyy This sounds like it is doing the full tree.  But I think it is not. Kai, nov'16

			// find routes between start and end stop
			transitPassengerRoute = tree.getTransitPassengerRoute(wrappedToNodes);

			if (transitPassengerRoute == null) {
//				return null; // yyyyyy why not return the direct walk leg?? kai/dz, mar'17
				return this.createDirectWalkLegList(null, fromFacility.getCoord(), toFacility.getCoord());
			}
			pathCost = transitPassengerRoute.getTravelCost();
		}

		double directWalkCost = getWalkDisutility(person, fromFacility.getCoord(), toFacility.getCoord());

		if (directWalkCost * getConfig().getDirectWalkFactor() < pathCost ) {
			return this.createDirectWalkLegList(null, fromFacility.getCoord(), toFacility.getCoord());
		}
		return convertPassengerRouteToLegList(departureTime, transitPassengerRoute, fromFacility.getCoord(), toFacility.getCoord(), person);
	}

}
