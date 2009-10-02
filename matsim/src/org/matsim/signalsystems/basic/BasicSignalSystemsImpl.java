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
package org.matsim.signalsystems.basic;

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
public class BasicSignalSystemsImpl implements BasicSignalSystems {

	private BasicSignalSystemsFactory builder = new BasicSignalSystemsFactory();
	
	private Map<Id, BasicSignalSystemDefinition> signalSystemDefinitions = new LinkedHashMap<Id, BasicSignalSystemDefinition>();
	private Map<Id, BasicSignalGroupDefinition> signalGroupDefinitions = new LinkedHashMap<Id, BasicSignalGroupDefinition>();;

	/**
	 * @see org.matsim.signalsystems.basic.BasicSignalSystems#getSignalSystemDefinitionsList()
	 */
	public List<BasicSignalSystemDefinition> getSignalSystemDefinitionsList() {
		List<BasicSignalSystemDefinition> r = new ArrayList<BasicSignalSystemDefinition>();
		r.addAll(this.signalSystemDefinitions.values());
		Collections.unmodifiableList(r);
		return Collections.unmodifiableList(r);
	}

	
	/**
	 * @see org.matsim.signalsystems.basic.BasicSignalSystems#getSignalGroupDefinitionsList()
	 */
	public List<BasicSignalGroupDefinition> getSignalGroupDefinitionsList() {
		List<BasicSignalGroupDefinition> r = new ArrayList<BasicSignalGroupDefinition>();
		r.addAll(this.signalGroupDefinitions.values());
		Collections.unmodifiableList(r);
		return Collections.unmodifiableList(r);
	}

	/**
	 * @see org.matsim.signalsystems.basic.BasicSignalSystems#addSignalSystemDefinition(org.matsim.signalsystems.basic.BasicSignalSystemDefinition)
	 */
	public void addSignalSystemDefinition(
			BasicSignalSystemDefinition lssdef) {
		this.signalSystemDefinitions.put(lssdef.getId(), lssdef);
	}
	
	/**
	 * @see org.matsim.signalsystems.basic.BasicSignalSystems#addSignalGroupDefinition(org.matsim.signalsystems.basic.BasicSignalGroupDefinition)
	 */
	public void addSignalGroupDefinition(BasicSignalGroupDefinition lsgdef) {
		this.signalGroupDefinitions.put(lsgdef.getId(), lsgdef);
	}


	public BasicSignalSystemsFactory getFactory() {
		return this.builder;
	}


	public Map<Id, BasicSignalSystemDefinition> getSignalSystemDefinitions() {
		return this.signalSystemDefinitions;
	}


	public Map<Id, BasicSignalGroupDefinition> getSignalGroupDefinitions() {
		return this.signalGroupDefinitions;
	}

	
}
