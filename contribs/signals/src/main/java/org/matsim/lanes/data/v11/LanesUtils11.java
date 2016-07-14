/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * LanesUtils11.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.lanes.data.v11;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.data.v20.Lane;

public class LanesUtils11 {
	/**
	 * Convenience method to create a format 11 lane with the given Id, the
	 * given length, the given number of represented lanes and the given Ids of
	 * the downstream links the lane leads to. The Lane is added to the
	 * LanesToLinkAssignment given as parameter.
	 *
	 * @param l2l
	 * @param factory
	 * @param laneId
	 * @param length
	 * @param noLanes
	 * @param toLinkIds
	 */
	public static void createAndAddLane11(LanesToLinkAssignment11 l2l, LaneDefinitionsFactory11 factory, Id<Lane> laneId,
			double length, 	double noLanes, Id<Link>... toLinkIds) {
		LaneData11 lane = factory.createLane(laneId);
		for (Id<Link> toLinkId : toLinkIds) {
			lane.addToLinkId(toLinkId);
		}
		lane.setStartsAtMeterFromLinkEnd(length);
		lane.setNumberOfRepresentedLanes(noLanes);
		l2l.addLane(lane);
	}
}
