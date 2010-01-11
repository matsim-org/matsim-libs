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

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

/**
 * @author dgrether
 */
public class SignalSystemPlanImpl implements SignalSystemPlan {

	private final Id id;
	private double startTime;
	private double endTime;
	private SortedMap<Id, SignalGroupSettings> groupConfigs;
	private Integer syncronizationOffset = 0;
	private Integer circulationTime = null;
	private Integer powerOnTime = null;
  private Integer powerOffTime = null;
	
	public SignalSystemPlanImpl(Id id) {
		this.id = id;
	}

	/**
	 * @see org.matsim.signalsystems.config.SignalSystemPlan#setStartTime(double)
	 */
	public void setStartTime(double seconds) {
		this.startTime = seconds;
	}

	/**
	 * @see org.matsim.signalsystems.config.SignalSystemPlan#setEndTime(double)
	 */
	public void setEndTime(double seconds) {
		this.endTime = seconds;
	}

	/**
	 * @see org.matsim.signalsystems.config.SignalSystemPlan#getId()
	 */
	public Id getId() {
		return id;
	}

	/**
	 * @see org.matsim.signalsystems.config.SignalSystemPlan#addLightSignalGroupConfiguration(org.matsim.signalsystems.config.SignalGroupSettings)
	 */
	public void addLightSignalGroupConfiguration(
			SignalGroupSettings groupConfig) {
		if (this.groupConfigs == null) {
			this.groupConfigs = new TreeMap<Id, SignalGroupSettings>();
		}
		this.groupConfigs.put(groupConfig.getReferencedSignalGroupId(), groupConfig);
	}
	
	/**
	 * @see org.matsim.signalsystems.config.SignalSystemPlan#getStartTime()
	 */
	public double getStartTime() {
		return startTime;
	}
	
	/**
	 * @see org.matsim.signalsystems.config.SignalSystemPlan#getEndTime()
	 */
	public double getEndTime() {
		return endTime;
	}
	
	/**
	 * @see org.matsim.signalsystems.config.SignalSystemPlan#getGroupConfigs()
	 */
	public SortedMap<Id, SignalGroupSettings> getGroupConfigs() {
		return groupConfigs;
	}

	/**
	 * @see org.matsim.signalsystems.config.SignalSystemPlan#setCycleTime(java.lang.Integer)
	 */
	public void setCycleTime(Integer circulationTimeSec) {
		this.circulationTime = circulationTimeSec;
	}
	
	/**
	 * @see org.matsim.signalsystems.config.SignalSystemPlan#setSynchronizationOffset(java.lang.Integer)
	 */
	public void setSynchronizationOffset(Integer seconds) {
		this.syncronizationOffset = seconds;
	}

	/**
	 * @see org.matsim.signalsystems.config.SignalSystemPlan#getSynchronizationOffset()
	 */
	public Integer getSynchronizationOffset() {
		return syncronizationOffset;
	}

	/**
	 * @see org.matsim.signalsystems.config.SignalSystemPlan#getCycleTime()
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
