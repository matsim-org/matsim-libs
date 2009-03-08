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
package org.matsim.basic.network;

import java.util.ArrayList;
import java.util.List;


/**
 * @author dgrether
 *
 */
public class BasicLaneDefinitions {
	
	private List<BasicLanesToLinkAssignment> lanesToLinkAssignments;

	public List<BasicLanesToLinkAssignment> getLanesToLinkAssignments() {
		return lanesToLinkAssignments;
	}

	/**
	 * @param assignment
	 */
	public void addLanesToLinkAssignment(BasicLanesToLinkAssignment assignment) {
		if (this.lanesToLinkAssignments == null) {
			this.lanesToLinkAssignments = new ArrayList<BasicLanesToLinkAssignment>();
		}
		this.lanesToLinkAssignments.add(assignment);
	}

	
}
