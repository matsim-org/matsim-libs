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

package org.matsim.lanes.data.v20;

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * @author dgrether
 */
public class LanesToLinkAssignment20Impl implements LanesToLinkAssignment20 {

	private final Id<Link> linkId;

	private final SortedMap<Id<Lane>, Lane> lanes = new TreeMap<>();

	public LanesToLinkAssignment20Impl(Id<Link> linkId) {
		this.linkId = linkId;
	}

	@Override
	public void addLane(Lane lane) {
		this.lanes.put(lane.getId(), lane);
	}

	@Override
	public Id<Link> getLinkId() {
		return linkId;
	}

	@Override
	public SortedMap<Id<Lane>, Lane> getLanes() {
		return this.lanes;
	}

}
