/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemDataImpl
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
package org.matsim.contrib.signals.data.signalsystems.v20;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;

/**
 * @author dgrether
 */
public class SignalSystemDataImpl implements SignalSystemData {

	private Id<SignalSystem> id;
	
	private Map<Id<Signal>, SignalData> signalDataMap = null;

	SignalSystemDataImpl(Id<SignalSystem> id) {
		this.id = id;
	}

	@Override
	public void addSignalData(SignalData signalData) {
		if (this.signalDataMap == null) {
			this.signalDataMap = new TreeMap<>();
		}
		this.signalDataMap.put(signalData.getId(), signalData);
	}

	@Override
	public Map<Id<Signal>, SignalData> getSignalData() {
		return this.signalDataMap;
	}

	@Override
	public Id<SignalSystem> getId() {
		return this.id;
	}

}
