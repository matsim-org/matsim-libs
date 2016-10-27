/* **********************************import java.util.List;

import org.matsim.interfaces.basic.v01.Id;
                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.data.Lane;
/**
 *
 * @author dgrether
 *
 */
@Deprecated
public interface LanesToLinkAssignment11 {

	public SortedMap<Id<Lane>, LaneData11> getLanes();

	public void addLane(LaneData11 lane);

	public Id<Link> getLinkId();

}