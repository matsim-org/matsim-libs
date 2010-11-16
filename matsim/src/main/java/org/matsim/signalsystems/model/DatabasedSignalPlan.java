/* *********************************************************************** *
 * project: org.matsim.*
 * DatabasedSignalPlan
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
package org.matsim.signalsystems.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;


/**
 * @author dgrether
 *
 */
public class DatabasedSignalPlan implements SignalPlan {
	private static final Logger log = Logger.getLogger(DatabasedSignalPlan.class);
	
	private SignalPlanData data;
	private int cylce;
	private int offset = 0;
	
	private Map<Integer, List<Id>> secondInPlanOnsetsMap = new HashMap<Integer, List<Id>>();

	private Map<Integer, List<Id>> secondInPlanDroppingsMap = new HashMap<Integer, List<Id>>();
	
	public DatabasedSignalPlan(SignalPlanData planData) {
		this.data = planData;
		this.init();
	}
	
	private void init(){
		if (this.data.getCycleTime() != null){
			this.cylce = data.getCycleTime();
		}
		else {
			throw new IllegalStateException("This implementation of SignalPlan works only with a cycle time");
		}
	
		if (this.data.getOffset() != null){
			this.offset = this.data.getOffset();
		}
		
		for (SignalGroupSettingsData sgdata : this.data.getSignalGroupSettingsDataByGroupId().values()){
			//onsets
			List<Id> onsetsSgIds = this.secondInPlanOnsetsMap.get(sgdata.getOnset());
			if (onsetsSgIds == null){
				onsetsSgIds = new ArrayList<Id>();
				this.secondInPlanOnsetsMap.put(sgdata.getOnset(), onsetsSgIds);
			}
			onsetsSgIds.add(sgdata.getSignalGroupId());
			//dropping
			List<Id> droppingSgIds = this.secondInPlanDroppingsMap.get(sgdata.getDropping());
			if (droppingSgIds == null){
				droppingSgIds = new ArrayList<Id>();
				this.secondInPlanDroppingsMap.put(sgdata.getDropping(), droppingSgIds);
			}
			droppingSgIds.add(sgdata.getSignalGroupId());
		}
		
	}

	@Override
	public List<Id> getDroppings(double timeSeconds) {
		Integer currentSecondInPlan = ((int) ((timeSeconds + this.offset) % this.cylce));
		return this.secondInPlanDroppingsMap.get(currentSecondInPlan);
	}

	@Override
	public List<Id> getOnsets(double timeSeconds) {
		Integer currentSecondInPlan = ((int) ((timeSeconds + this.offset) % this.cylce));
		return this.secondInPlanOnsetsMap.get(currentSecondInPlan);
	}

	
	@Override
	public Double getEndTime() {
		return this.data.getEndTime();
	}

	@Override
	public Double getStartTime() {
		return this.data.getStartTime();
	}

	@Override
	public Id getId() {
		return this.data.getId();
	}

}
