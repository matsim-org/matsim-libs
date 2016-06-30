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

package playground.polettif.publicTransitMapping.mapping.linkCandidateCreation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * A possible link for a TransitStopFacility. A LinkCandidate contains
 * theoretically a link and the parent StopFacility. However, all
 * values besides Coord are stored as primitive/Id since one might
 * be working with multiple separated networks.
 *
 * @author polettif
 */
public interface LinkCandidate extends Comparable<LinkCandidate> {

	String getId();

	Id<TransitStopFacility> getParentStopFacilityId();

	Id<Link> getLinkId();

	Id<Node> getToNodeId();

	Id<Node> getFromNodeId();

	Coord getToNodeCoord();

	Coord getFromNodeCoord();

	double getStopFacilityDistance();

	double getLinkTravelCost();

	boolean isLoopLink();

	/**
	 * Should return a value greater than 1 if the other LinkCandidate
	 * has a lower priority.
	 */
	int compareTo(LinkCandidate other);

	/**
	 * @return the link candidates priority compared to all other
	 * link candidates for the same stop and transport mode. The priority
	 * is scaled 0..1 (1 being high priority).
	 */
	double getPriority();

	void setPriority(double priority);
}
