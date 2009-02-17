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

package org.matsim.basic.signalsystemsconfig;

import java.util.HashMap;
import java.util.Map;

import org.matsim.basic.v01.Id;

/**
 * @author dgrether
 */
public class BasicSignalSystemPlan {

	private Id id;
	private double startTime;
	private double endTime;
	private Map<Id, BasicSignalGroupConfiguration> groupConfigs;
	private Double syncronizationOffset = null;
	private Double circulationTime = null;

	public BasicSignalSystemPlan(Id id) {
		this.id = id;
	}

	public void setStartTime(double seconds) {
		this.startTime = seconds;
	}

	public void setEndTime(double seconds) {
		this.endTime = seconds;
	}

	public Id getId() {
		return id;
	}

	public void addLightSignalGroupConfiguration(
			BasicSignalGroupConfiguration groupConfig) {
		if (this.groupConfigs == null) {
			this.groupConfigs = new HashMap<Id, BasicSignalGroupConfiguration>();
		}
		this.groupConfigs.put(groupConfig.getReferencedSignalGroupId(), groupConfig);
	}
	
	public double getStartTime() {
		return startTime;
	}
	
	public double getEndTime() {
		return endTime;
	}
	
	public Map<Id, BasicSignalGroupConfiguration> getGroupConfigs() {
		return groupConfigs;
	}

	public void setCirculationTime(Double seconds) {
		this.circulationTime = seconds;
	}
	
	public void setSyncronizationOffset(Double seconds) {
		this.syncronizationOffset = seconds;
	}

	public Double getSyncronizationOffset() {
		return syncronizationOffset;
	}

	public Double getCirculationTime() {
		return circulationTime;
	}

}
