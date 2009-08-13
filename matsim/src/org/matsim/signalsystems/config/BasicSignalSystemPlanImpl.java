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

package org.matsim.signalsystems.config;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.basic.v01.Id;

/**
 * @author dgrether
 */
public class BasicSignalSystemPlanImpl implements BasicSignalSystemPlan {

	private Id id;
	private double startTime;
	private double endTime;
	private Map<Id, BasicSignalGroupSettings> groupConfigs;
	private Integer syncronizationOffset = 0;
	private Integer circulationTime = null;
	private Integer powerOnTime = null;
  private Integer powerOffTime = null;
	
	public BasicSignalSystemPlanImpl(Id id) {
		this.id = id;
	}

	/**
	 * @see org.matsim.signalsystems.config.BasicSignalSystemPlan#setStartTime(double)
	 */
	public void setStartTime(double seconds) {
		this.startTime = seconds;
	}

	/**
	 * @see org.matsim.signalsystems.config.BasicSignalSystemPlan#setEndTime(double)
	 */
	public void setEndTime(double seconds) {
		this.endTime = seconds;
	}

	/**
	 * @see org.matsim.signalsystems.config.BasicSignalSystemPlan#getId()
	 */
	public Id getId() {
		return id;
	}

	/**
	 * @see org.matsim.signalsystems.config.BasicSignalSystemPlan#addLightSignalGroupConfiguration(org.matsim.signalsystems.config.BasicSignalGroupSettings)
	 */
	public void addLightSignalGroupConfiguration(
			BasicSignalGroupSettings groupConfig) {
		if (this.groupConfigs == null) {
			this.groupConfigs = new HashMap<Id, BasicSignalGroupSettings>();
		}
		this.groupConfigs.put(groupConfig.getReferencedSignalGroupId(), groupConfig);
	}
	
	/**
	 * @see org.matsim.signalsystems.config.BasicSignalSystemPlan#getStartTime()
	 */
	public double getStartTime() {
		return startTime;
	}
	
	/**
	 * @see org.matsim.signalsystems.config.BasicSignalSystemPlan#getEndTime()
	 */
	public double getEndTime() {
		return endTime;
	}
	
	/**
	 * @see org.matsim.signalsystems.config.BasicSignalSystemPlan#getGroupConfigs()
	 */
	public Map<Id, BasicSignalGroupSettings> getGroupConfigs() {
		return groupConfigs;
	}

	/**
	 * @see org.matsim.signalsystems.config.BasicSignalSystemPlan#setCycleTime(java.lang.Integer)
	 */
	public void setCycleTime(Integer circulationTimeSec) {
		this.circulationTime = circulationTimeSec;
	}
	
	/**
	 * @see org.matsim.signalsystems.config.BasicSignalSystemPlan#setSynchronizationOffset(java.lang.Integer)
	 */
	public void setSynchronizationOffset(Integer seconds) {
		this.syncronizationOffset = seconds;
	}

	/**
	 * @see org.matsim.signalsystems.config.BasicSignalSystemPlan#getSynchronizationOffset()
	 */
	public Integer getSynchronizationOffset() {
		return syncronizationOffset;
	}

	/**
	 * @see org.matsim.signalsystems.config.BasicSignalSystemPlan#getCycleTime()
	 */
	public Integer getCycleTime() {
		return circulationTime;
	}

	
	public Integer getPowerOnTime() {
		return powerOnTime;
	}

	
	public void setPowerOnTime(Integer powerOnTime) {
		this.powerOnTime = powerOnTime;
	}

	
	public Integer getPowerOffTime() {
		return powerOffTime;
	}

	
	public void setPowerOffTime(Integer powerOffTime) {
		this.powerOffTime = powerOffTime;
	}

}
