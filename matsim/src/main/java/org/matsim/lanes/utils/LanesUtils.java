/* *********************************************************************** *
 * project: org.matsim.*
 * LanesUtils
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.lanes.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.lanes.data.v11.Lane;
import org.matsim.lanes.data.v11.LaneDefinitionsFactory;
import org.matsim.lanes.data.v11.LanesToLinkAssignment;

/**
 * @author dgrether
 * 
 */
public final class LanesUtils {

	/**
	 * Convenience method to create a Lane with the given Id, the given length, the given number of represented
	 * lanes and the given Ids of the downstream links the lane leads to. The Lane is added to the LanesToLinkAssignment
	 * given as parameter.
	 */
	public static void createAndAddLane(LanesToLinkAssignment l2l, LaneDefinitionsFactory factory, Id laneId, 
			double length, 	double noLanes, Id... toLinkIds) {
		Lane lane = factory.createLane(laneId);
		for (Id toLinkId : toLinkIds) {
			lane.addToLinkId(toLinkId);
		}
		lane.setStartsAtMeterFromLinkEnd(length);
		lane.setNumberOfRepresentedLanes(noLanes);
		l2l.addLane(lane);
	}

}
