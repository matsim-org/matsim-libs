/* *********************************************************************** *
 * project: org.matsim.*
 * BasicLaneDefinitionsBuilderImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.lanes.data.v20;

import org.matsim.api.core.v01.Id;


/**
 * 
 * @author dgrether
 * @see org.matsim.lanes.data.v20.LaneDefinitionsFactoryV2
 */
public class LaneDefinitionsFactoryV2Impl implements LaneDefinitionsFactoryV2 {
	/**
	 * @see org.matsim.lanes.data.v20.LaneDefinitionsFactoryV2#createLanesToLinkAssignment(org.matsim.api.core.v01.Id)
	 */
	@Override
	public LanesToLinkAssignmentV2 createLanesToLinkAssignment(Id linkIdReference) {
		return new LanesToLinkAssignmentV2Impl(linkIdReference);
	}
	/**
	 * @see org.matsim.lanes.data.v20.LaneDefinitionsFactoryV2#createLane(org.matsim.api.core.v01.Id)
	 */
	@Override
	public LaneDataV2 createLane(Id id) {
		return new LaneDataV2Impl(id);
	}
}
