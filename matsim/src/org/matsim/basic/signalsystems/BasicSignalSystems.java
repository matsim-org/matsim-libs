/* *********************************************************************** *
 * project: org.matsim.																																* 
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.basic.signalsystems;

import java.util.ArrayList;
import java.util.List;


/**
 * @author dgrether
 *
 */
public class BasicSignalSystems {

	private List<BasicLanesToLinkAssignment> lanesToLinkAssignments;
	private List<BasicSignalSystemDefinition> lightSignalSystemDefinitions;
	private List<BasicSignalGroupDefinition> lightSignalGroupDefinitions;

	
	
	
	
	public List<BasicLanesToLinkAssignment> getLanesToLinkAssignments() {
		return lanesToLinkAssignments;
	}

	
	public List<BasicSignalSystemDefinition> getSignalSystemDefinitions() {
		return lightSignalSystemDefinitions;
	}

	
	public List<BasicSignalGroupDefinition> getSignalGroupDefinitions() {
		return lightSignalGroupDefinitions;
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

	/**
	 * @param lssdef
	 */
	public void addSignalSystemDefinition(
			BasicSignalSystemDefinition lssdef) {
		if (this.lightSignalSystemDefinitions == null) {
			this.lightSignalSystemDefinitions = new ArrayList<BasicSignalSystemDefinition>();
		}
		this.lightSignalSystemDefinitions.add(lssdef);
	}
	
	public void addSignalGroupDefinition(BasicSignalGroupDefinition lsgdef) {
		if (this.lightSignalGroupDefinitions == null) {
			this.lightSignalGroupDefinitions = new ArrayList<BasicSignalGroupDefinition>();
		}
		this.lightSignalGroupDefinitions.add(lsgdef);
	}

	
}
