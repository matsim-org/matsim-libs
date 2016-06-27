/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.polettif.publicTransitMapping.mapping.pseudoRouter;

import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import playground.polettif.publicTransitMapping.mapping.linkCandidateCreation.LinkCandidate;

import java.util.Collection;
import java.util.List;

/**
 * A pseudo graph with {@link PseudoRouteStop}s as nodes. Edges connect
 * two PseudoRouteStops. The graph is used to calculate the least cost
 * path on from a dummy source to a dummy destination. This least cost
 * path contains the best suited PseudoRouteStops.
 *
 * @author polettif
 */
public interface PseudoGraph {

	void addEdge(int orderOfFirstStop, TransitRouteStop fromTransitRouteStop, LinkCandidate fromLinkCandidate, TransitRouteStop toTransitRouteStop, LinkCandidate toLinkCandidate, double pathTravelCost);

	void addDummyEdges(List<TransitRouteStop> transitRouteStops, Collection<LinkCandidate> firstStopLinkCandidates, Collection<LinkCandidate> lastStopLinkCandidates);

	List<PseudoRouteStop> getLeastCostStopSequence();

}
