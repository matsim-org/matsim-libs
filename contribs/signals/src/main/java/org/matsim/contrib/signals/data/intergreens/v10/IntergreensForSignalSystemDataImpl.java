/* *********************************************************************** *
 * project: org.matsim.*
 * IntergreenTimesDataImpl
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
package org.matsim.contrib.signals.data.intergreens.v10;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.contrib.signals.data.ambertimes.v10.IntergreensForSignalSystemData;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;


/**
 * @author dgrether
 *
 */
public class IntergreensForSignalSystemDataImpl implements IntergreensForSignalSystemData {

	private Id<SignalSystem> signalSystemId;
	private Map<Id<SignalGroup>, Map<Id<SignalGroup>, Integer>> endingGroupToBeginningGroupTimeMap = new HashMap<>();
	private Map<Id<SignalGroup>, Map<Id<SignalGroup>, Integer>> beginningGroupToEndingGroupTimeMap = new HashMap<>();
	
	public IntergreensForSignalSystemDataImpl(Id<SignalSystem> signalSystemId) {
		this.signalSystemId = signalSystemId;
	}

	@Override
	public List<Tuple<Id<SignalGroup>, Id<SignalGroup>>> getEndingBeginningSignalGroupKeys(){
		List<Tuple<Id<SignalGroup>, Id<SignalGroup>>> list = new ArrayList<>();
		for (Entry<Id<SignalGroup>, Map<Id<SignalGroup>, Integer>> e : this.endingGroupToBeginningGroupTimeMap.entrySet()){
			for (Id<SignalGroup> beginningId : e.getValue().keySet()){
				list.add(new Tuple<>(e.getKey(), beginningId));
			}
		}
		return list;
	}
	
	@Override
	public Integer getIntergreenTime(Id<SignalGroup> endingSignalGroupId, Id<SignalGroup> beginningSignalGroupId) {
		Map<Id<SignalGroup>, Integer> beginningSgMap = this.endingGroupToBeginningGroupTimeMap.get(endingSignalGroupId);
		if (beginningSgMap != null){
			return beginningSgMap.get(beginningSignalGroupId);
		}
		return null;
	}

	@Override
	public void setIntergreenTime(Integer timeSeconds, Id<SignalGroup> endingSignalGroupId,
			Id<SignalGroup> beginningSignalGroupId) {
		Map<Id<SignalGroup>, Integer> endingSgMap = this.endingGroupToBeginningGroupTimeMap.get(endingSignalGroupId);
		if (endingSgMap == null){
			endingSgMap = new HashMap<>();
			this.endingGroupToBeginningGroupTimeMap.put(endingSignalGroupId, endingSgMap);
		}
		endingSgMap.put(beginningSignalGroupId, timeSeconds);
		
		Map<Id<SignalGroup>, Integer> beginningSgMap = this.beginningGroupToEndingGroupTimeMap.get(beginningSignalGroupId);
		if (beginningSgMap == null){
			beginningSgMap = new HashMap<>();
			this.beginningGroupToEndingGroupTimeMap.put(endingSignalGroupId, beginningSgMap);
		}
		beginningSgMap.put(beginningSignalGroupId, timeSeconds);
	}

	@Override
	public Id<SignalSystem> getSignalSystemId() {
		return this.signalSystemId;
	}


	@Override
	public Map<Id<SignalGroup>, Integer> getEndSignalGroupTimesForBeginningGroup(Id<SignalGroup> id) {
		return this.beginningGroupToEndingGroupTimeMap.get(id);
	}


}
