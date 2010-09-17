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
package org.matsim.signalsystems.data.signalsystems.v20;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * @author dgrether
 */
public class SignalSystemDataImpl implements SignalSystemData {

	private Id id;
	
	private Map<Id, SignalData> signalDataMap = null;

	public SignalSystemDataImpl(Id id) {
		this.id = id;
	}

	@Override
	public void addSignalData(SignalData signalData) {
		if (this.signalDataMap == null) {
			this.signalDataMap = new HashMap<Id, SignalData>();
		}
		this.signalDataMap.put(signalData.getId(), signalData);
	}

	@Override
	public Map<Id, SignalData> getSignalData() {
		return this.signalDataMap;
	}

	@Override
	public Id getId() {
		return this.id;
	}

}
