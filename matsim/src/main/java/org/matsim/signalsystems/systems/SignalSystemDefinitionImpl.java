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

package org.matsim.signalsystems.systems;

import org.matsim.api.core.v01.Id;

/**
 * @author dgrether
 */
public class SignalSystemDefinitionImpl implements SignalSystemDefinition {

  private Id id;
  private Double defaultCirculationTime = null;
  private Double syncronizationOffset = null;
  private Double defaultInterimTime = null;
	
  public SignalSystemDefinitionImpl(Id id) {
  	this.id = id;
  }
  
	/**
	 * @see org.matsim.signalsystems.systems.SignalSystemDefinition#getId()
	 */
	public Id getId() {
		return id;
	}
	
	/**
	 * @see org.matsim.signalsystems.systems.SignalSystemDefinition#getDefaultCycleTime()
	 */
	public Double getDefaultCycleTime() {
		return defaultCirculationTime;
	}
	
	/**
	 * @see org.matsim.signalsystems.systems.SignalSystemDefinition#setDefaultCycleTime(double)
	 */
	public void setDefaultCycleTime(Double defaultCirculationTime) {
		this.defaultCirculationTime = defaultCirculationTime;
	}
	
	/**
	 * @see org.matsim.signalsystems.systems.SignalSystemDefinition#getDefaultSynchronizationOffset()
	 */
	public Double getDefaultSynchronizationOffset() {
		return syncronizationOffset;
	}
	
	/**
	 * @see org.matsim.signalsystems.systems.SignalSystemDefinition#setDefaultSynchronizationOffset(double)
	 */
	public void setDefaultSynchronizationOffset(Double syncronizationOffset) {
		this.syncronizationOffset = syncronizationOffset;
	}
	
	/**
	 * @see org.matsim.signalsystems.systems.SignalSystemDefinition#getDefaultInterGreenTime()
	 */
	public Double getDefaultInterGreenTime() {
		return defaultInterimTime;
	}
	
	/**
	 * @see org.matsim.signalsystems.systems.SignalSystemDefinition#setDefaultInterGreenTime(double)
	 */
	public void setDefaultInterGreenTime(Double defaultInterimTime) {
		this.defaultInterimTime = defaultInterimTime;
	}

}
