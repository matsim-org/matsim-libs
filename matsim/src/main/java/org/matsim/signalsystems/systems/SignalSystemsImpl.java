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
package org.matsim.signalsystems.systems;

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
public class SignalSystemsImpl implements SignalSystems {

	private final SignalSystemsFactory builder = new SignalSystemsFactory();
	
	private final SortedMap<Id, SignalSystemDefinition> signalSystemDefinitions = new TreeMap<Id, SignalSystemDefinition>();
	private final SortedMap<Id, SignalGroupDefinition> signalGroupDefinitions = new TreeMap<Id, SignalGroupDefinition>();;

	/**
	 * @see org.matsim.signalsystems.systems.SignalSystems#getSignalSystemDefinitionsList()
	 */
	public List<SignalSystemDefinition> getSignalSystemDefinitionsList() {
		List<SignalSystemDefinition> r = new ArrayList<SignalSystemDefinition>();
		r.addAll(this.signalSystemDefinitions.values());
		Collections.unmodifiableList(r);
		return Collections.unmodifiableList(r);
	}

	
	/**
	 * @see org.matsim.signalsystems.systems.SignalSystems#getSignalGroupDefinitionsList()
	 */
	public List<SignalGroupDefinition> getSignalGroupDefinitionsList() {
		List<SignalGroupDefinition> r = new ArrayList<SignalGroupDefinition>();
		r.addAll(this.signalGroupDefinitions.values());
		Collections.unmodifiableList(r);
		return Collections.unmodifiableList(r);
	}

	/**
	 * @see org.matsim.signalsystems.systems.SignalSystems#addSignalSystemDefinition(org.matsim.signalsystems.systems.SignalSystemDefinition)
	 */
	public void addSignalSystemDefinition(
			SignalSystemDefinition lssdef) {
		this.signalSystemDefinitions.put(lssdef.getId(), lssdef);
	}
	
	/**
	 * @see org.matsim.signalsystems.systems.SignalSystems#addSignalGroupDefinition(org.matsim.signalsystems.systems.SignalGroupDefinition)
	 */
	public void addSignalGroupDefinition(SignalGroupDefinition lsgdef) {
		this.signalGroupDefinitions.put(lsgdef.getId(), lsgdef);
	}


	public SignalSystemsFactory getFactory() {
		return this.builder;
	}


	public SortedMap<Id, SignalSystemDefinition> getSignalSystemDefinitions() {
		return this.signalSystemDefinitions;
	}


	public SortedMap<Id, SignalGroupDefinition> getSignalGroupDefinitions() {
		return this.signalGroupDefinitions;
	}

	
}
