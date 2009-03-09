/* *********************************************************************** *
 * project: org.matsim.																																* 
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
package org.matsim.basic.signalsystems;

import java.util.ArrayList;
import java.util.List;


/**
 * @author dgrether
 *
 */
public class BasicSignalSystemsImpl implements BasicSignalSystems {

	private BasicSignalSystemsBuilder builder = new BasicSignalSystemsBuilder();
	
	private List<BasicSignalSystemDefinition> lightSignalSystemDefinitions;
	private List<BasicSignalGroupDefinition> lightSignalGroupDefinitions;

	/**
	 * @see org.matsim.basic.signalsystems.BasicSignalSystems#getSignalSystemDefinitions()
	 */
	public List<BasicSignalSystemDefinition> getSignalSystemDefinitions() {
		return lightSignalSystemDefinitions;
	}

	
	/**
	 * @see org.matsim.basic.signalsystems.BasicSignalSystems#getSignalGroupDefinitions()
	 */
	public List<BasicSignalGroupDefinition> getSignalGroupDefinitions() {
		return lightSignalGroupDefinitions;
	}

	/**
	 * @see org.matsim.basic.signalsystems.BasicSignalSystems#addSignalSystemDefinition(org.matsim.basic.signalsystems.BasicSignalSystemDefinition)
	 */
	public void addSignalSystemDefinition(
			BasicSignalSystemDefinition lssdef) {
		if (this.lightSignalSystemDefinitions == null) {
			this.lightSignalSystemDefinitions = new ArrayList<BasicSignalSystemDefinition>();
		}
		this.lightSignalSystemDefinitions.add(lssdef);
	}
	
	/**
	 * @see org.matsim.basic.signalsystems.BasicSignalSystems#addSignalGroupDefinition(org.matsim.basic.signalsystems.BasicSignalGroupDefinition)
	 */
	public void addSignalGroupDefinition(BasicSignalGroupDefinition lsgdef) {
		if (this.lightSignalGroupDefinitions == null) {
			this.lightSignalGroupDefinitions = new ArrayList<BasicSignalGroupDefinition>();
		}
		this.lightSignalGroupDefinitions.add(lsgdef);
	}


	public BasicSignalSystemsBuilder getSignalSystemsBuilder() {
		return this.builder;
	}

	
}
