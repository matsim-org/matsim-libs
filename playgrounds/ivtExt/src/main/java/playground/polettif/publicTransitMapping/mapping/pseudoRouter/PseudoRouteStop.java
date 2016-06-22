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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.Map;

/**
 * TODO doc
 */
public interface PseudoRouteStop extends Identifiable<PseudoRouteStop>, Comparable<PseudoRouteStop> {

	Id<TransitStopFacility> getParentStopFacilityId();

	LinkCandidate getLinkCandidate();

	Coord getCoord();

	boolean isBlockingLane();

	boolean awaitsDepartureTime();

	String getFacilityName();

	String getStopPostAreaId();

	double getArrivalOffset();

	double getDepartureOffset();

	@Deprecated
	double getLinkTravelCost();

	int compareTo(PseudoRouteStop other);

	Id<Link> getLinkId();

	/**
	 * Used for Dijkstra in {@link PseudoGraph}
	 */
	Map<PseudoRouteStop, Double> getNeighbours();
	/**
	 * Used for Dijkstra in {@link PseudoGraph}
	 */
	double getTravelCostToSource();
	/**
	 * Used for Dijkstra in {@link PseudoGraph}
	 */
	void setTravelCostToSource(double alternateDist);
	/**
	 * Used for Dijkstra in {@link PseudoGraph}
	 */
	PseudoRouteStop getClosestPrecedingRouteStop();
	/**
	 * Used for Dijkstra in {@link PseudoGraph}
	 */
	void setClosestPrecedingRouteSTop(PseudoRouteStop stop);

}
