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

/**
 * TODO doc
 */
public interface LinkCandidate extends Comparable<LinkCandidate> {

	Coord getToNodeCoord();

	Coord getFromNodeCoord();

	String getToNodeIdStr();

	String getFromNodeIdStr();

	String getLinkIdStr();

	String getId();

	double getStopFacilityDistance();

	double getLinkTravelCost();

	/**
	 * @return the link candidates priority compared to all other
	 * link candidates for the same stop and transport mode. The priority
	 * is scaled 0..1 (1 being high priority).
	 */
	double getPriority();

	void setPriority(double priority);

	int compareTo(LinkCandidate other);
}
