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
import org.matsim.api.core.v01.network.Link;


/**
 * 
 * @author dgrether
 * @see org.matsim.lanes.data.v20.LaneDefinitionsFactory20
 */
public class LaneDefinitionsFactory20Impl implements LaneDefinitionsFactory20 {
	/**
	 * @see org.matsim.lanes.data.v20.LaneDefinitionsFactory20#createLanesToLinkAssignment(org.matsim.api.core.v01.Id)
	 */
	@Override
	public LanesToLinkAssignment20 createLanesToLinkAssignment(Id<Link> linkIdReference) {
		return new LanesToLinkAssignment20Impl(linkIdReference);
	}
	/**
	 * @see org.matsim.lanes.data.v20.LaneDefinitionsFactory20#createLane(org.matsim.api.core.v01.Id)
	 */
	@Override
	public Lane createLane(Id<Lane> id) {
		return new LaneData20Impl(id);
	}
}
