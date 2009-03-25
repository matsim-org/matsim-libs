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

package org.matsim.core.basic.signalsystemsconfig;

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
	private Integer syncronizationOffset = null;
	private Integer circulationTime = null;

	public BasicSignalSystemPlanImpl(Id id) {
		this.id = id;
	}

	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemPlan#setStartTime(double)
	 */
	public void setStartTime(double seconds) {
		this.startTime = seconds;
	}

	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemPlan#setEndTime(double)
	 */
	public void setEndTime(double seconds) {
		this.endTime = seconds;
	}

	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemPlan#getId()
	 */
	public Id getId() {
		return id;
	}

	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemPlan#addLightSignalGroupConfiguration(org.matsim.core.basic.signalsystemsconfig.BasicSignalGroupSettings)
	 */
	public void addLightSignalGroupConfiguration(
			BasicSignalGroupSettings groupConfig) {
		if (this.groupConfigs == null) {
			this.groupConfigs = new HashMap<Id, BasicSignalGroupSettings>();
		}
		this.groupConfigs.put(groupConfig.getReferencedSignalGroupId(), groupConfig);
	}
	
	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemPlan#getStartTime()
	 */
	public double getStartTime() {
		return startTime;
	}
	
	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemPlan#getEndTime()
	 */
	public double getEndTime() {
		return endTime;
	}
	
	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemPlan#getGroupConfigs()
	 */
	public Map<Id, BasicSignalGroupSettings> getGroupConfigs() {
		return groupConfigs;
	}

	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemPlan#setCirculationTime(java.lang.Integer)
	 */
	public void setCirculationTime(Integer circulationTimeSec) {
		this.circulationTime = circulationTimeSec;
	}
	
	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemPlan#setSyncronizationOffset(java.lang.Integer)
	 */
	public void setSyncronizationOffset(Integer seconds) {
		this.syncronizationOffset = seconds;
	}

	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemPlan#getSyncronizationOffset()
	 */
	public Integer getSyncronizationOffset() {
		return syncronizationOffset;
	}

	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemPlan#getCirculationTime()
	 */
	public Integer getCirculationTime() {
		return circulationTime;
	}

}
