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
package org.matsim.contrib.signals.data.signalgroups.v20;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;


/**
 * @author dgrether
 *
 */
public class SignalGroupsDataImpl implements SignalGroupsData {

	private Map<Id<SignalSystem>, Map<Id<SignalGroup>, SignalGroupData>> signalGroupsDataBySystemId = new TreeMap<>();
	
	private SignalGroupsDataFactory factory;
	
	public SignalGroupsDataImpl(){
		this.factory=new SignalGroupsDataFactoryImpl();
	}
	
	@Override
	public void addSignalGroupData(SignalGroupData signalGroupData) {
		Id<SignalSystem> signalSystemId = signalGroupData.getSignalSystemId();
		Map<Id<SignalGroup>, SignalGroupData> map = this.signalGroupsDataBySystemId.get(signalSystemId);
		if (map == null){
			map = new TreeMap<>();
			this.signalGroupsDataBySystemId.put(signalSystemId, map);
		}
		map.put(signalGroupData.getId(), signalGroupData);
	}

	@Override
	public Map<Id<SignalGroup>, SignalGroupData> getSignalGroupDataBySystemId(Id<SignalSystem> signalSystemId) {
		return this.signalGroupsDataBySystemId.get(signalSystemId);
	}

	@Override
	public SignalGroupsDataFactory getFactory() {
		return this.factory;
	}

	@Override
	public Map<Id<SignalSystem>, Map<Id<SignalGroup>, SignalGroupData>> getSignalGroupDataBySignalSystemId() {
		return signalGroupsDataBySystemId;
	}

	@Override
	public void setFactory(SignalGroupsDataFactory factory) {
		this.factory = factory;
	}

	
}
