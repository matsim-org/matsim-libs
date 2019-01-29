/* *********************************************************************** *
 * project: org.matsim.*
 * SignalDataImpl
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

package org.matsim.contrib.signals.data.ambertimes.v10;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;

/**
 * @author jbischoff
 * @author dgrether
 */
public class AmberTimeDataImpl implements AmberTimeData {

	private Integer defaultRedAmberTime;
	private Integer defaultAmberTime;
	private Map<Id<Signal>, Integer> signalAmberMap;
	private Map<Id<Signal>, Integer> signalRedAmberMap;
	private Id<SignalSystem> signalSystemId;

	AmberTimeDataImpl(Id<SignalSystem> signalSystemId) {
		this.signalSystemId = signalSystemId;
		signalAmberMap = new HashMap<>();
		signalRedAmberMap = new HashMap<>();
	}

	@Override
	public Integer getAmberOfSignal(Id<Signal> signalId) {
		if (signalAmberMap.containsKey(signalId)) {
			return signalAmberMap.get(signalId);
		}
		else

			return defaultAmberTime;
	}

	@Override
	public Integer getDefaultAmber() {
		return this.defaultAmberTime;
	}

	@Override
	public Integer getDefaultRedAmber() {
		return this.defaultRedAmberTime;
	}

	@Override
	public Integer getRedAmberOfSignal(Id<Signal> signalId) {
		if (signalRedAmberMap.containsKey(signalId)) {
			return signalRedAmberMap.get(signalId);
		}
		else

			return defaultRedAmberTime;
	}

	@Override
	public Map<Id<Signal>, Integer> getSignalAmberMap() {

		return signalAmberMap;
	}

	@Override
	public Map<Id<Signal>, Integer> getSignalRedAmberMap() {
		return signalRedAmberMap;
	}

	@Override
	public Id<SignalSystem> getSignalSystemId() {
		return signalSystemId;
	}

	@Override
	public void setAmberTimeOfSignal(Id<Signal> signalId, Integer seconds) {
		signalAmberMap.put(signalId, seconds);
	}

	@Override
	public void setDefaultAmber(Integer seconds) {
		this.defaultAmberTime = seconds;
	}

	@Override
	public void setDefaultRedAmber(Integer seconds) {
		this.defaultRedAmberTime = seconds;
	}

	@Override
	public void setRedAmberTimeOfSignal(Id<Signal> signalId, Integer seconds) {
		signalRedAmberMap.put(signalId, seconds);
	}

}
