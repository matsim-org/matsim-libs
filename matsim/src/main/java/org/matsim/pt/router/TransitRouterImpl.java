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

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.TransitLeastCostPathTree.InitialNode;
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
			final PreparedTransitSchedule preparedTransitSchedule,
			final TransitRouterNetwork routerNetwork,
			final TravelTime travelTime,
			final TransitTravelDisutility travelDisutility) {
		super( config, preparedTransitSchedule, routerNetwork, travelTime, travelDisutility) ;
	}

	@Override
	public List<Leg> calcRoute(final Facility<?> fromFacility, final Facility<?> toFacility, final double departureTime, final Person person) {
		// find possible start stops
		Map<Node, InitialNode> wrappedFromNodes = this.locateWrappedNearestTransitNodes(person, fromFacility.getCoord(), departureTime);
		// find possible end stops
		Map<Node, InitialNode> wrappedToNodes = this.locateWrappedNearestTransitNodes(person, toFacility.getCoord(), departureTime);
		
		double pathCost = Double.POSITIVE_INFINITY ;
		Path p = null ;

		if ( wrappedFromNodes != null && wrappedToNodes != null ) {

			TransitLeastCostPathTree tree = new TransitLeastCostPathTree(getTransitRouterNetwork(), getTravelDisutility(), getTravelTime(),
					wrappedFromNodes, wrappedToNodes, person);
			// yyyyyy This sounds like it is doing the full tree.  But I think it is not. Kai, nov'16

			// find routes between start and end stop
			p = tree.getPath(wrappedToNodes);

			if (p == null) {
				return null; // yyyyyy why not return the direct walk leg?? kai/dz, mar'17
			}
			pathCost = p.travelCost + wrappedFromNodes.get(p.nodes.get(0)).initialCost + wrappedToNodes.get(p.nodes.get(p.nodes.size() - 1)).initialCost;
		}

		double directWalkCost = getWalkDisutility(person, fromFacility.getCoord(), toFacility.getCoord());

		if (directWalkCost * getConfig().getDirectWalkFactor() < pathCost ) {
			return this.createDirectWalkLegList(null, fromFacility.getCoord(), toFacility.getCoord());
		}
		Gbl.assertNotNull(p); // this is now a bit confused since I do not understand the logic, see above.  kai, mar'17
		return convertPathToLegList(departureTime, p, fromFacility.getCoord(), toFacility.getCoord(), person);
	}

}
