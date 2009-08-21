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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.basic.v01.Id;


/**
 * @author dgrether
 *
 */
public class BasicLaneDefinitionsImpl implements BasicLaneDefinitions {
	
	private Map<Id, BasicLanesToLinkAssignment> lanesToLinkAssignments =  new LinkedHashMap<Id, BasicLanesToLinkAssignment>();

	private BasicLaneDefinitionsBuilder builder = new BasicLaneDefinitionsBuilderImpl();
	
	/**
	 * @see org.matsim.lanes.basic.BasicLaneDefinitions#getLanesToLinkAssignmentsList()
	 */
	public List<BasicLanesToLinkAssignment> getLanesToLinkAssignmentsList() {
		List<BasicLanesToLinkAssignment> ret = new ArrayList<BasicLanesToLinkAssignment>();
		ret.addAll(this.lanesToLinkAssignments.values());
		return Collections.unmodifiableList(ret);
	}
	
	public Map<Id, BasicLanesToLinkAssignment> getLanesToLinkAssignments() {
		return this.lanesToLinkAssignments;
	}

	/**
	 * @see org.matsim.lanes.basic.BasicLaneDefinitions#addLanesToLinkAssignment(org.matsim.lanes.basic.BasicLanesToLinkAssignment)
	 */
	public void addLanesToLinkAssignment(BasicLanesToLinkAssignment assignment) {
		if (this.lanesToLinkAssignments == null) {
			this.lanesToLinkAssignments = new LinkedHashMap<Id, BasicLanesToLinkAssignment>();
		}
		this.lanesToLinkAssignments.put(assignment.getLinkId(), assignment);
	}
	
	/**
	 * @see org.matsim.lanes.basic.BasicLaneDefinitions#getBuilder()
	 */
	public BasicLaneDefinitionsBuilder getBuilder(){
		return this.builder;
	}


	
}
