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
package org.matsim.lanes.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class BasicLaneDefinitionsImpl implements BasicLaneDefinitions {
	
	private SortedMap<Id, BasicLanesToLinkAssignment> lanesToLinkAssignments =  new TreeMap<Id, BasicLanesToLinkAssignment>();

	private final BasicLaneDefinitionsFactory builder = new BasicLaneDefinitionsFactoryImpl();
	
	/**
	 * @see org.matsim.lanes.basic.BasicLaneDefinitions#getLanesToLinkAssignmentsList()
	 */
	public List<BasicLanesToLinkAssignment> getLanesToLinkAssignmentsList() {
		List<BasicLanesToLinkAssignment> ret = new ArrayList<BasicLanesToLinkAssignment>();
		ret.addAll(this.lanesToLinkAssignments.values());
		return Collections.unmodifiableList(ret);
	}
	
	public SortedMap<Id, BasicLanesToLinkAssignment> getLanesToLinkAssignments() {
		return this.lanesToLinkAssignments;
	}

	/**
	 * @see org.matsim.lanes.basic.BasicLaneDefinitions#addLanesToLinkAssignment(org.matsim.lanes.basic.BasicLanesToLinkAssignment)
	 */
	public void addLanesToLinkAssignment(BasicLanesToLinkAssignment assignment) {
		if (this.lanesToLinkAssignments == null) {
			this.lanesToLinkAssignments = new TreeMap<Id, BasicLanesToLinkAssignment>();
		}
		this.lanesToLinkAssignments.put(assignment.getLinkId(), assignment);
	}
	
	/**
	 * @see org.matsim.lanes.basic.BasicLaneDefinitions#getFactory()
	 */
	public BasicLaneDefinitionsFactory getFactory(){
		return this.builder;
	}


	
}
