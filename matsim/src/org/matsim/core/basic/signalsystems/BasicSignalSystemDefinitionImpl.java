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

package org.matsim.core.basic.signalsystems;

import org.matsim.api.basic.v01.Id;

/**
 * @author dgrether
 */
public class BasicSignalSystemDefinitionImpl implements BasicSignalSystemDefinition {

  private Id id;
  private double defaultCirculationTime;
  private double syncronizationOffset;
  private double defaultInterimTime;
	
  public BasicSignalSystemDefinitionImpl(Id id) {
  	this.id = id;
  }
  
	/**
	 * @see org.matsim.core.basic.signalsystems.BasicSignalSystemDefinition#getId()
	 */
	public Id getId() {
		return id;
	}
	
	/**
	 * @see org.matsim.core.basic.signalsystems.BasicSignalSystemDefinition#getDefaultCycleTime()
	 */
	public double getDefaultCycleTime() {
		return defaultCirculationTime;
	}
	
	/**
	 * @see org.matsim.core.basic.signalsystems.BasicSignalSystemDefinition#setDefaultCycleTime(double)
	 */
	public void setDefaultCycleTime(double defaultCirculationTime) {
		this.defaultCirculationTime = defaultCirculationTime;
	}
	
	/**
	 * @see org.matsim.core.basic.signalsystems.BasicSignalSystemDefinition#getDefaultSynchronizationOffset()
	 */
	public double getDefaultSynchronizationOffset() {
		return syncronizationOffset;
	}
	
	/**
	 * @see org.matsim.core.basic.signalsystems.BasicSignalSystemDefinition#setDefaultSynchronizationOffset(double)
	 */
	public void setDefaultSynchronizationOffset(double syncronizationOffset) {
		this.syncronizationOffset = syncronizationOffset;
	}
	
	/**
	 * @see org.matsim.core.basic.signalsystems.BasicSignalSystemDefinition#getDefaultInterGreenTime()
	 */
	public double getDefaultInterGreenTime() {
		return defaultInterimTime;
	}
	
	/**
	 * @see org.matsim.core.basic.signalsystems.BasicSignalSystemDefinition#setDefaultInterGreenTime(double)
	 */
	public void setDefaultInterGreenTime(double defaultInterimTime) {
		this.defaultInterimTime = defaultInterimTime;
	}

}
