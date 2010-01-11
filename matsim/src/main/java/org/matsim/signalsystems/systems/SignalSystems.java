/* *********************************************************************** *
 * project: org.matsim.																																* 
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
package org.matsim.signalsystems.systems;

import java.util.List;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimToplevelContainer;

public interface SignalSystems extends MatsimToplevelContainer {
	
	public SignalSystemsFactory getFactory();

	public SortedMap<Id, SignalSystemDefinition> getSignalSystemDefinitions();

	public SortedMap<Id, SignalGroupDefinition> getSignalGroupDefinitions();

	/**
	 * 
	 * @deprecated use getSignalSystemDefinitions()
	 */
	@Deprecated
	public List<SignalSystemDefinition> getSignalSystemDefinitionsList();
	/**
	 * 
	 * @deprecated use getSignalGroupDefinitions()
	 */
	@Deprecated
	public List<SignalGroupDefinition> getSignalGroupDefinitionsList();

	/**
	 * @param lssdef
	 */
	public void addSignalSystemDefinition(SignalSystemDefinition lssdef);

	public void addSignalGroupDefinition(SignalGroupDefinition lsgdef);

}