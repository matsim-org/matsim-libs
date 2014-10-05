/* *********************************************************************** *
 * project: org.matsim.*
 * BasicLaneDefinitionBuilder
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
package org.matsim.lanes.data.v11;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.lanes.data.v20.Lane;


/**
 * Builder for the content of BasicLaneDefinitions
 * @author dgrether
 */
public interface LaneDefinitionsFactory11 extends MatsimFactory {

	/**
	 * 
	 * @param linkIdReference id of the links the lanes of the created object belong to
	 * @return An empty instance of LanesToLinkAssignment for the Link with the Id given as parameter
	 */
	public LanesToLinkAssignment11 createLanesToLinkAssignment(Id<Link> linkIdReference);
	/**
	 * Creates an instance of BasicLane with the id given as parameter.
	 * @param laneId
	 * @return
	 */
	public LaneData11 createLane(Id<Lane> laneId);
}
