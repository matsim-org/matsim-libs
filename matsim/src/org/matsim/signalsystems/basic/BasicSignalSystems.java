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
package org.matsim.signalsystems.basic;

import java.util.List;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.internal.MatsimToplevelContainer;

public interface BasicSignalSystems extends MatsimToplevelContainer {
	
	public BasicSignalSystemsBuilder getBuilder();

	public Map<Id, BasicSignalSystemDefinition> getSignalSystemDefinitions();

	public Map<Id, BasicSignalGroupDefinition> getSignalGroupDefinitions();

	/**
	 * 
	 * @deprecated use getSignalSystemDefinitions()
	 */
	@Deprecated
	public List<BasicSignalSystemDefinition> getSignalSystemDefinitionsList();
	/**
	 * 
	 * @deprecated use getSignalGroupDefinitions()
	 */
	@Deprecated
	public List<BasicSignalGroupDefinition> getSignalGroupDefinitionsList();

	/**
	 * @param lssdef
	 */
	public void addSignalSystemDefinition(BasicSignalSystemDefinition lssdef);

	public void addSignalGroupDefinition(BasicSignalGroupDefinition lsgdef);

}