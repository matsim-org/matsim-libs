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
package org.matsim.trafficlights.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.basic.v01.Id;


/**
 * @author dgrether
 *
 */
public class SignalSystemPlan {

	private Id id;
	private double startTime;
	private double stopTime;
	private int circulationTime;
	private int syncTime;
	private int powerOnTime;
	private int powerOffTime;
	private Map<Id, SignalGroupSettings> signalGroupSettings;

	public SignalSystemPlan(Id id) {
		this.id = id;
		this.signalGroupSettings = new HashMap<Id, SignalGroupSettings>();
	}

	public void addSignalGroupSettings(
			SignalGroupSettings signalGroupSetting) {
		this.signalGroupSettings.put(signalGroupSetting.getSignalGroupDefinition().getId(), signalGroupSetting);
	}

	public List<SignalGroupDefinition> getSignalGroupDefinitions() {
		List<SignalGroupDefinition> ret = new ArrayList<SignalGroupDefinition>(this.signalGroupSettings.size());
		for (SignalGroupSettings s : this.signalGroupSettings.values()) {
			ret.add(s.getSignalGroupDefinition());
		}
		return ret;
	}

	public Map<Id, SignalGroupSettings> getSignalGroupSettings() {
		return this.signalGroupSettings;
	}

	public Id getId() {
		return  this.id;
	}

	public void setStartTime(double t) {
		this.startTime = t;
	}

	public void setStopTime(double t) {
		this.stopTime = t;
	}

	public void setCirculationTime(int t) {
		this.circulationTime = t;
	}
	/**
	 * @return the syncTime
	 */
	public int getSyncTime() {
		return this.syncTime;
	}
	/**
	 * @param syncTime the syncTime to set
	 */
	public void setSyncTime(int syncTime) {
		this.syncTime = syncTime;
	}
	/**
	 * @return the powerOnTime
	 */
	public int getPowerOnTime() {
		return this.powerOnTime;
	}
	/**
	 * @param powerOnTime the powerOnTime to set
	 */
	public void setPowerOnTime(int powerOnTime) {
		this.powerOnTime = powerOnTime;
	}
	/**
	 * @return the powerOffTime
	 */
	public int getPowerOffTime() {
		return this.powerOffTime;
	}
	/**
	 * @param powerOffTime the powerOffTime to set
	 */
	public void setPowerOffTime(int powerOffTime) {
		this.powerOffTime = powerOffTime;
	}
	/**
	 * @return the startTime
	 */
	public double getStartTime() {
		return this.startTime;
	}
	/**
	 * @return the stopTime
	 */
	public double getStopTime() {
		return this.stopTime;
	}
	/**
	 * @return the circulationTime
	 */
	public int getCirculationTime() {
		return this.circulationTime;
	}


}
