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

package org.matsim.lanes.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.basic.v01.Id;

/**
 * @author dgrether
 */
public class BasicLanesToLinkAssignmentImpl implements BasicLanesToLinkAssignment {

	private Id linkId;

	private Map<Id, BasicLane> lanes = new LinkedHashMap<Id, BasicLane>();
	
	/**
	 * @param linkId
	 */
	public BasicLanesToLinkAssignmentImpl(Id linkId) {
		this.linkId = linkId;
	}

	/**
	 * @see org.matsim.lanes.basic.BasicLanesToLinkAssignment#getLanesList()
	 */
	public List<BasicLane> getLanesList() {
		List<BasicLane> r = new ArrayList<BasicLane>();
		r.addAll(this.lanes.values());
		return Collections.unmodifiableList(r);
	}
	
	/**
	 * @see org.matsim.lanes.basic.BasicLanesToLinkAssignment#addLane(org.matsim.lanes.basic.BasicLane)
	 */
	public void addLane(BasicLane lane) {
		this.lanes.put(lane.getId(), lane); 
	}
	
	/**
	 * @see org.matsim.lanes.basic.BasicLanesToLinkAssignment#getLinkId()
	 */
	public Id getLinkId() {
		return linkId;
	}

	public Map<Id, BasicLane> getLanes() {
		return this.lanes;
	}

}
