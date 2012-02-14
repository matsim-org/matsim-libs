/* *********************************************************************** *
 * project: org.matsim.*
 * LaneDefinitionsFactoryV1Impl
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
package org.matsim.lanes.data.v11;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class LaneDefinitionsFactoryImpl implements LaneDefinitionsFactory {

	@Override
	public LanesToLinkAssignment createLanesToLinkAssignment(Id linkId) {
		return new LanesToLinkAssignmentV1Impl(linkId);
	}

	@Override
	public Lane createLane(Id laneId) {
		return new LaneImpl(laneId);
	}

}
