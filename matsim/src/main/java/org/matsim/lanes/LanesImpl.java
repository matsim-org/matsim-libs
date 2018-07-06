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
package org.matsim.lanes;

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;


/**
 * @author dgrether
 */
 final class LanesImpl implements Lanes {

	private SortedMap<Id<Link>, LanesToLinkAssignment> lanesToLinkAssignments =  new TreeMap<>();

	private LanesFactory builder = new LanesFactoryImpl();

	LanesImpl(){}

	@Override
	public SortedMap<Id<Link>, LanesToLinkAssignment> getLanesToLinkAssignments() {
		return this.lanesToLinkAssignments;
	}

	@Override
	public void addLanesToLinkAssignment(LanesToLinkAssignment assignment) {
		this.lanesToLinkAssignments.put(assignment.getLinkId(), assignment);
	}

	@Override
	public LanesFactory getFactory(){
		return this.builder;
	}

}
