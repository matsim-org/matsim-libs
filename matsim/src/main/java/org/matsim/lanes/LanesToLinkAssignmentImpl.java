/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

package org.matsim.lanes;

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

/**
 * @author dgrether
 */
public class LanesToLinkAssignmentImpl implements LanesToLinkAssignment {

	private final Id linkId;

	private final SortedMap<Id, Lane> lanes = new TreeMap<Id, Lane>();

	public LanesToLinkAssignmentImpl(Id linkId) {
		this.linkId = linkId;
	}

	public void addLane(Lane lane) {
		this.lanes.put(lane.getId(), lane);
	}

	public Id getLinkId() {
		return linkId;
	}

	public SortedMap<Id, Lane> getLanes() {
		return this.lanes;
	}

}
