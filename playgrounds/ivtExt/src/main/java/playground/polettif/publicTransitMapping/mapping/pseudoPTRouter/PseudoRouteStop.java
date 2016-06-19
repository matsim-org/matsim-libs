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

package playground.polettif.publicTransitMapping.mapping.pseudoPTRouter;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Identifiable;

import java.util.Map;

/**
 * TODO doc
 */
public interface PseudoRouteStop extends Identifiable<PseudoRouteStop>, Comparable<PseudoRouteStop> {
	String getParentStopFacilityId();

	Coord getCoord();

	boolean isBlockingLane();

	boolean awaitsDepartureTime();

	String getLinkIdStr();

	String getFacilityName();

	String getStopPostAreaId();

	double getArrivalOffset();

	double getDepartureOffset();

	double getLinkTravelCost();

	Map<PseudoRouteStop, Double> getNeighbours();

	double getTravelCostToSource();
	void setTravelCostToSource(double alternateDist);

	PseudoRouteStop getClosestPrecedingRouteStop();
	void setClosestPrecedingRouteSTop(PseudoRouteStop stop);

	int compareTo(PseudoRouteStop other);
}
