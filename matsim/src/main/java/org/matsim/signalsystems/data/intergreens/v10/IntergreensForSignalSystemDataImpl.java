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
package org.matsim.signalsystems.data.intergreens.v10;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;


/**
 * @author dgrether
 *
 */
public class IntergreensForSignalSystemDataImpl implements IntergreensForSignalSystemData {

	private Id signalSystemId;
	private Map<Id, Map<Id, Integer>> intergreenTimesMap = new HashMap<Id, Map<Id, Integer>>();
	
	public IntergreensForSignalSystemDataImpl(Id signalSystemId) {
		this.signalSystemId = signalSystemId;
	}

	public List<Tuple<Id, Id>> getEndingBeginningSignalGroupKeys(){
		List<Tuple<Id, Id>> list = new ArrayList<Tuple<Id, Id>>();
		for (Entry<Id, Map<Id, Integer>> e : this.intergreenTimesMap.entrySet()){
			for (Id beginningId : e.getValue().keySet()){
				list.add( new Tuple<Id, Id>(e.getKey(), beginningId));
			}
		}
		return list;
	}
	
	@Override
	public Integer getIntergreenTime(Id endingSignalGroupId, Id beginningSignalGroupId) {
		Map<Id, Integer> beginningSgMap = this.intergreenTimesMap.get(endingSignalGroupId);
		if (beginningSgMap != null){
			return beginningSgMap.get(beginningSignalGroupId);
		}
		return null;
	}

	@Override
	public void setIntergreenTime(Integer timeSeconds, Id endingSignalGroupId,
			Id beginningSignalGroupId) {
		Map<Id, Integer> endingSgMap = this.intergreenTimesMap.get(endingSignalGroupId);
		if (endingSgMap == null){
			endingSgMap = new HashMap<Id, Integer>();
			this.intergreenTimesMap.put(endingSignalGroupId, endingSgMap);
		}
		endingSgMap.put(beginningSignalGroupId, timeSeconds);
	}

	@Override
	public Id getSignalSystemId() {
		return this.signalSystemId;
	}

}
