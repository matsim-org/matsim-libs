/* *********************************************************************** *
 * project: org.matsim.*
 * SignalPlanDataImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.data.signalcontrol.v20;

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;


/**
 * @author dgrether
 *
 */
public class SignalPlanDataImpl implements SignalPlanData {

	private Id<SignalPlan> id;
	private Integer cycletime;
	private double endtime = 0.0;
	private int offset;
	private SortedMap<Id<SignalGroup>, SignalGroupSettingsData> signalGroupSettingsBySignalGroupId;
	private double starttime = 0.0;

	public SignalPlanDataImpl(Id<SignalPlan> id) {
		this.id = id;
	}

	@Override
	public void addSignalGroupSettings(SignalGroupSettingsData signalGroupSettings) {
		if (this.signalGroupSettingsBySignalGroupId == null) {
			this.signalGroupSettingsBySignalGroupId = new TreeMap<>();
		}
		this.signalGroupSettingsBySignalGroupId.put(signalGroupSettings.getSignalGroupId(), signalGroupSettings);
	}

	@Override
	public Integer getCycleTime() {
		return this.cycletime;
	}

	@Override
	public double getEndTime() {
		return this.endtime;
	}

	@Override
	public Id<SignalPlan> getId() {
		return this.id;
	}

	@Override
	public int getOffset() {
		return this.offset;
	}

	@Override
	public SortedMap<Id<SignalGroup>, SignalGroupSettingsData> getSignalGroupSettingsDataByGroupId() {
		return this.signalGroupSettingsBySignalGroupId;
	}

	@Override
	public double getStartTime() {
		return this.starttime;
	}

	@Override
	public void setCycleTime(Integer cycleTime) {
		this.cycletime = cycleTime;
	}

	@Override
	public void setEndTime(Double seconds) {
		this.endtime = seconds;
	}

	@Override
	public void setOffset(int seconds) {
		this.offset = seconds;
	}

	@Override
	public void setStartTime(Double seconds) {
		this.starttime = seconds;
	}

}
