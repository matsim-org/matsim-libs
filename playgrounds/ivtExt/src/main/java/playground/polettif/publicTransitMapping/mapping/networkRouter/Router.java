/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.polettif.publicTransitMapping.mapping.networkRouter;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import playground.polettif.publicTransitMapping.mapping.PseudoRouting;
import playground.polettif.publicTransitMapping.mapping.linkCandidateCreation.LinkCandidate;

/**
 * A Router that calculates the least cost path on a network.
 *
 * @author polettif
 */
public interface Router extends TravelDisutility, TravelTime {

    /**
     * @param fromLinkCandidate  Node to route from...
     * @param toLinkCandidate    Node to route to...
     * @return  Least cost path.
     */
    LeastCostPathCalculator.Path calcLeastCostPath(LinkCandidate fromLinkCandidate, LinkCandidate toLinkCandidate);

    Network getNetwork();

	/**
	 * @return The minimal travel cost between two TransitRouteStops
	 */
	double getMinimalTravelCost(TransitRouteStop fromStop, TransitRouteStop toStop);

	/**
	 * @return The travel cost for the given link
	 */
	double getLinkTravelCost(Link link);

	/**
	 * If {@link PseudoRouting} needs to add an artificial link to the network, this method returns
	 * the freespeed value.
	 */
	double getArtificialLinkFreeSpeed(double maxAllowedTravelCost, LinkCandidate fromLinkCandidate, LinkCandidate toLinkCandidate);

	/**
	 * If {@link PseudoRouting} needs to add an artificial link to the network, this method returns
	 * the link length.
	 */
	double getArtificialLinkLength(double maxAllowedTravelCost, LinkCandidate linkCandidateCurrent, LinkCandidate linkCandidateNext);

	LeastCostPathCalculator.Path calcLeastCostPath(Node toNode, Node fromNode);
}
