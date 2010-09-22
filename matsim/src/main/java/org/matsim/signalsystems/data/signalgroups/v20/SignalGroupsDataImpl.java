/* *********************************************************************** *
 * project: org.matsim.*
 * SignalGroupsDataImpl
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
package org.matsim.signalsystems.data.signalgroups.v20;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class SignalGroupsDataImpl implements SignalGroupsData {

	private Map<Id, Map<Id, SignalGroupData>> signalGroupsDataBySystemId = new HashMap<Id, Map<Id, SignalGroupData>>();
	
	@Override
	public void addSignalGroupData(SignalGroupData signalGroupData) {
		Id signalSystemId = signalGroupData.getSignalSystemId();
		Map<Id, SignalGroupData> map = this.signalGroupsDataBySystemId.get(signalSystemId);
		if (map == null){
			map = new HashMap<Id, SignalGroupData>();
			this.signalGroupsDataBySystemId.put(signalSystemId, map);
		}
		map.put(signalGroupData.getId(), signalGroupData);
	}

	@Override
	public Map<Id, SignalGroupData> getSignalGroupDataBySystemId(Id signalSystemId) {
		return this.signalGroupsDataBySystemId.get(signalSystemId);
	}

	@Override
	public SignalGroupsDataFactory getFactory() {
		return null;
	}

	
}
