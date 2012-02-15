/* *********************************************************************** *
 * project: org.matsim.*
 * BasicLaneDefinitions
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

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 */
public class LaneDefinitions20Impl implements LaneDefinitions20 {

	private SortedMap<Id, LanesToLinkAssignment20> lanesToLinkAssignments =  new TreeMap<Id, LanesToLinkAssignment20>();

	private final LaneDefinitionsFactory20 builder = new LaneDefinitionsFactory20Impl();

	@Override
	public SortedMap<Id, LanesToLinkAssignment20> getLanesToLinkAssignments() {
		return this.lanesToLinkAssignments;
	}

	@Override
	public void addLanesToLinkAssignment(LanesToLinkAssignment20 assignment) {
		this.lanesToLinkAssignments.put(assignment.getLinkId(), assignment);
	}

	@Override
	public LaneDefinitionsFactory20 getFactory(){
		return this.builder;
	}

}
