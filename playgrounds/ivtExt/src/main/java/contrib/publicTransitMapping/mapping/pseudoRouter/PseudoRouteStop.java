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

package contrib.publicTransitMapping.mapping.pseudoRouter;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import contrib.publicTransitMapping.mapping.linkCandidateCreation.LinkCandidate;

import java.util.Map;

/**
 * A PseudoRouteStop is used as node in the PseudoGraph. It is a container
 * for a {@link TransitRouteStop} with a {@link LinkCandidate}
 * <p/>
 * LinkCandidates are made for each stop facility. Since one
 * StopFacility might be accessed twice in the same TransitRoute,
 * unique LinkCandidates for each TransitRouteStop are needed. This
 * is achieved via this class.
 *
 * @author polettif
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

	int compareTo(PseudoRouteStop other);

	Id<Link> getLinkId();

	/**
	 * Used for Dijkstra in {@link PseudoGraph}. Returns a map
	 * with neighboring PseudoRouteStops and the travel cost to
	 * reach them.
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
