/* *********************************************************************** *
 * project: org.matsim.*
 * DgPhase
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.controller.sylvia;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.model.SignalGroup;

/**
 * container for overlapping signal group settings
 * 
 * there are two kinds of phase definitions: unsimplified phases this kind of phase contains all overlapping signals for each signal in the phase i.e. different phases cannot overlap each other
 * simplified phases its phase end is defined by the dropping of the largest setting starting at the phase starting time different phases of this kind cannot overlap each other
 * 
 * @author dgrether
 * @author tthunig
 *
 */
public final class FixedTimeSignalPhase {

	private static final Logger log = Logger.getLogger(FixedTimeSignalPhase.class);
	private Integer on = null;
	private Integer off = null;
	private Map<Id<SignalGroup>, SignalGroupSettingsData> signalGroupSettingsByGroupId = new HashMap<>();

	// private boolean mixedPhase = false;

	public FixedTimeSignalPhase(Integer phaseOn, Integer phaseDrop) {
		log.debug("created phase from " + phaseOn + " to " + phaseDrop);
		this.on = phaseOn;
		this.off = phaseDrop;
	}

	public FixedTimeSignalPhase(Integer phaseOn, Integer phaseDrop, Map<Id<SignalGroup>, SignalGroupSettingsData> phaseSignals) {
		this(phaseOn, phaseDrop);
		this.signalGroupSettingsByGroupId = phaseSignals;
	}

	public void setPhaseStartSecond(Integer on) {
		this.on = on;
	}

	public void setPhaseEndSecond(Integer off) {
		this.off = off;
	}

	public Integer getPhaseStartSecond() {
		return this.on;
	}

	public Integer getPhaseEndSecond() {
		return this.off;
	}

	public void setSignalGroupSettingsByGroupId(Map<Id<SignalGroup>, SignalGroupSettingsData> signalGroupSettingsByGroupId) {
		this.signalGroupSettingsByGroupId = signalGroupSettingsByGroupId;
	}

	public void addSignalGroupSettingsData(SignalGroupSettingsData settings) {
		log.debug("  adding settings to phase: " + settings.getSignalGroupId() + " on: " + settings.getOnset() + " drop " + settings.getDropping());
		if (settings.getOnset() < this.on || settings.getDropping() > off) {
			throw new IllegalStateException("SignalGroupSettings longer than phase length!");
		}
		this.signalGroupSettingsByGroupId.put(settings.getSignalGroupId(), settings);
	}

	public Map<Id<SignalGroup>, SignalGroupSettingsData> getSignalGroupSettingsByGroupId() {
		return this.signalGroupSettingsByGroupId;
	}

}