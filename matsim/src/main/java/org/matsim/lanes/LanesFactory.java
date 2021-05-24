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
package org.matsim.lanes;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.internal.MatsimFactory;


/**
 * Builder for the content of BasicLaneDefinitions
 * @author dgrether
 */
public interface LanesFactory extends MatsimFactory {

	/**
	 * 
	 * @param linkIdReference id of the links the lanes of the created object belong to
	 * @return An empty instance of LanesToLinkAssignment for the Link with the Id given as parameter
	 */
	LanesToLinkAssignment createLanesToLinkAssignment(Id<Link> linkIdReference);
	/**
	 * Creates an instance of BasicLane with the id given as parameter.
	 * @param laneId
	 * @return
	 */
	Lane createLane(Id<Lane> laneId);
}
