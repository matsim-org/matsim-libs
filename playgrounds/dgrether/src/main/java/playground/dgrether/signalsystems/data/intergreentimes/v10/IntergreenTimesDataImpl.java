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
package playground.dgrether.signalsystems.data.intergreentimes.v10;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class IntergreenTimesDataImpl implements IntergreenTimesData {

	private Map<Id, Map<Id, Integer>> intergreenTimesMap = new HashMap<Id, Map<Id, Integer>>();;
	
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
		Map<Id, Integer> beginningSgMap = this.intergreenTimesMap.get(endingSignalGroupId);
		if (beginningSgMap == null){
			beginningSgMap = new HashMap<Id, Integer>();
			this.intergreenTimesMap.put(endingSignalGroupId, beginningSgMap);
		}
		beginningSgMap.put(endingSignalGroupId, timeSeconds);
	}

}
