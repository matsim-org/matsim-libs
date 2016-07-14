/* *********************************************************************** *
 * project: org.matsim.*
 * LaneDefinitionsV1Impl
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

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;


/**
 * @author dgrether
 *
 */
public class LaneDefinitions11Impl implements LaneDefinitions11 {

	private SortedMap<Id<Link>, LanesToLinkAssignment11> lanesToLinkAssignments  =  new TreeMap<>();

	private LaneDefinitionsFactory11 factory = new LaneDefinitionsFactory11Impl();
	
	@Override
	public SortedMap<Id<Link>, LanesToLinkAssignment11> getLanesToLinkAssignments() {
		return this.lanesToLinkAssignments;
	}

	@Override
	public void addLanesToLinkAssignment(LanesToLinkAssignment11 assignment) {
		this.lanesToLinkAssignments.put(assignment.getLinkId(), assignment);
	}

	@Override
	public LaneDefinitionsFactory11 getFactory() {
		return factory;
	}

	@Override
	public void setFactory(LaneDefinitionsFactory11 factory) {
		this.factory = factory;
	}
}
