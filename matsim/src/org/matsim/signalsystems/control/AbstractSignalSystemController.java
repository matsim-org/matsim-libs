/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractSignalSystemController
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
package org.matsim.signalsystems.control;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.queuesim.SignalEngine;
import org.matsim.signalsystems.basic.BasicSignalGroupDefinition;




/**
 * Abstract implementation of SignalSystemController interface that provides behaviour
 * useful for most control mechanisms.
 * 
 * @author dgrether
 *
 */
public  abstract class AbstractSignalSystemController implements SignalSystemController {

	private Double defaultCycleTime = null;
	private Double defaultInterGreenTime = null;
	private Double defaultSynchronizationOffset = null;
	
	private SignalEngine signalEngine;
	private Map<BasicSignalGroupDefinition, SignalGroupState> state;
	private Map<Id, BasicSignalGroupDefinition> groups;
	
	protected AbstractSignalSystemController() {
		this.groups = new LinkedHashMap<Id, BasicSignalGroupDefinition> ();
		this.state = new LinkedHashMap<BasicSignalGroupDefinition, SignalGroupState>();
	}
	
	public Double getDefaultCycleTime() {
		return defaultCycleTime;
	}
	
	public void setDefaultCycleTime(Double defaultCycleTime) {
		this.defaultCycleTime = defaultCycleTime;
	}
	
	public Double getDefaultInterGreenTime() {
		return defaultInterGreenTime;
	}
	
	public void setDefaultInterGreenTime(Double defaultInterGreenTime) {
		this.defaultInterGreenTime = defaultInterGreenTime;
	}
	
	public Double getDefaultSynchronizationOffset() {
		return defaultSynchronizationOffset;
	}
	
	public void setDefaultSynchronizationOffset(Double defaultSynchronizationOffset) {
		this.defaultSynchronizationOffset = defaultSynchronizationOffset;
	}
	
  public 	Map<Id, BasicSignalGroupDefinition> getSignalGroups(){
  	return this.groups;
  }
  
  public Map<BasicSignalGroupDefinition, SignalGroupState> getSignalGroupStates() {
  	return this.state;
  }

	
	public SignalEngine getSignalEngine() {
		return signalEngine;
	}

	
	public void setSignalEngine(SignalEngine signalEngine) {
		this.signalEngine = signalEngine;
	}
}
