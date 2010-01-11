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
package org.matsim.lanes;

import org.matsim.api.core.v01.Id;


/**
 * 
 * @author dgrether
 * @see org.matsim.lanes.LaneDefinitionsFactory
 */
public class LaneDefinitionsFactoryImpl implements LaneDefinitionsFactory {
	/**
	 * @see org.matsim.lanes.LaneDefinitionsFactory#createLanesToLinkAssignment(org.matsim.api.core.v01.Id)
	 */
	public LanesToLinkAssignment createLanesToLinkAssignment(Id linkIdReference) {
		return new LanesToLinkAssignmentImpl(linkIdReference);
	}
	/**
	 * @see org.matsim.lanes.LaneDefinitionsFactory#createLane(org.matsim.api.core.v01.Id)
	 */
	public Lane createLane(Id id) {
		return new LaneImpl(id);
	}
}
