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
package org.matsim.basic.lightsignalsystems;

import java.util.ArrayList;
import java.util.List;


/**
 * @author dgrether
 *
 */
public class BasicLightSignalSystems {

	private List<BasicLanesToLinkAssignment> lanesToLinkAssignments;
	private List<BasicLightSignalSystemDefinition> lightSignalSystemDefinitions;
	private List<BasicLightSignalGroupDefinition> lightSignalGroupDefinitions;

	
	
	
	
	public List<BasicLanesToLinkAssignment> getLanesToLinkAssignments() {
		return lanesToLinkAssignments;
	}

	
	public List<BasicLightSignalSystemDefinition> getLightSignalSystemDefinitions() {
		return lightSignalSystemDefinitions;
	}

	
	public List<BasicLightSignalGroupDefinition> getLightSignalGroupDefinitions() {
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
	public void addLightSignalSystemDefinition(
			BasicLightSignalSystemDefinition lssdef) {
		if (this.lightSignalSystemDefinitions == null) {
			this.lightSignalSystemDefinitions = new ArrayList<BasicLightSignalSystemDefinition>();
		}
		this.lightSignalSystemDefinitions.add(lssdef);
	}
	
	public void addLightSignalGroupDefinition(BasicLightSignalGroupDefinition lsgdef) {
		if (this.lightSignalGroupDefinitions == null) {
			this.lightSignalGroupDefinitions = new ArrayList<BasicLightSignalGroupDefinition>();
		}
		this.lightSignalGroupDefinitions.add(lsgdef);
	}

	
}
