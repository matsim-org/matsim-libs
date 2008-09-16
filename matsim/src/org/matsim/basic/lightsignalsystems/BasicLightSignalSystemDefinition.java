/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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

import org.matsim.basic.v01.Id;


/**
 * @author dgrether
 *
 */
public class BasicLightSignalSystemDefinition {

  private Id id;
  private double defaultCirculationTime;
  private double syncronizationOffset;
  private double defaultInterimTime;
	
  public BasicLightSignalSystemDefinition(Id id) {
  	this.id = id;
  }
  
	public Id getId() {
		return id;
	}
	
	public double getDefaultCirculationTime() {
		return defaultCirculationTime;
	}
	
	public void setDefaultCirculationTime(double defaultCirculationTime) {
		this.defaultCirculationTime = defaultCirculationTime;
	}
	
	public double getDefaultSyncronizationOffset() {
		return syncronizationOffset;
	}
	
	public void setDefaultSyncronizationOffset(double syncronizationOffset) {
		this.syncronizationOffset = syncronizationOffset;
	}
	
	public double getDefaultInterimTime() {
		return defaultInterimTime;
	}
	
	public void setDefaultInterimTime(double defaultInterimTime) {
		this.defaultInterimTime = defaultInterimTime;
	}

}
