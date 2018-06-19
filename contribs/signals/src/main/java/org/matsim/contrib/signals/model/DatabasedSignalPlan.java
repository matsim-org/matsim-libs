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
package org.matsim.contrib.signals.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;


/**
 * @author dgrether
 *
 */
public class DatabasedSignalPlan implements SignalPlan {
//	private static final Logger log = Logger.getLogger(DatabasedSignalPlan.class);
	
	private SignalPlanData data;
	private int cycle;
	
	private Map<Integer, List<Id<SignalGroup>>> secondInPlanOnsetsMap = new HashMap<>();

	private Map<Integer, List<Id<SignalGroup>>> secondInPlanDroppingsMap = new HashMap<>();
	
	public DatabasedSignalPlan(SignalPlanData planData) {
		this.data = planData;
		this.init();
	}
	
	private void init(){
		if (this.data.getCycleTime() != null){
			this.cycle = data.getCycleTime();
		}
		else {
			throw new IllegalStateException("This implementation of SignalPlan works only with a cycle time");
		}
	
		int offset = 0;
		if (this.data.getOffset() != null){
			offset = this.data.getOffset();
		}
		
		for (SignalGroupSettingsData sgdata : this.data.getSignalGroupSettingsDataByGroupId().values()){
//			log.error("  SignalGroup " +  sgdata.getSignalGroupId());
			//do nothing if onset == dropping or all time green is set
			if (! ( (sgdata.getOnset() == sgdata.getDropping()) || 
					(sgdata.getOnset() % this.cycle == 0 && sgdata.getDropping() % this.cycle == 0))){
				int onset = sgdata.getOnset();
				onset = getPositiveModuloByCycleTime(offset+onset);
				//onsets
				List<Id<SignalGroup>> onsetsSgIds = this.secondInPlanOnsetsMap.get(onset);
				if (onsetsSgIds == null){
					onsetsSgIds = new ArrayList<>();
					this.secondInPlanOnsetsMap.put(onset, onsetsSgIds);
				}
				onsetsSgIds.add(sgdata.getSignalGroupId());
				//dropping
				int dropping = sgdata.getDropping();
				dropping = getPositiveModuloByCycleTime(offset+dropping);
				List<Id<SignalGroup>> droppingSgIds = this.secondInPlanDroppingsMap.get(dropping);
				if (droppingSgIds == null){
					droppingSgIds = new ArrayList<>();
					this.secondInPlanDroppingsMap.put(dropping, droppingSgIds);
				}
				droppingSgIds.add(sgdata.getSignalGroupId());
			}
		}
	}

	private int getPositiveModuloByCycleTime(int dividend) {
		int modulo = dividend % this.cycle;
		if (modulo < 0) modulo += this.cycle;
		return modulo;
	}
	

	@Override
	public List<Id<SignalGroup>> getDroppings(double timeSeconds) {
		Integer currentSecondInPlan = ((int) (timeSeconds % this.cycle));
		return this.secondInPlanDroppingsMap.get(currentSecondInPlan);
	}

	@Override
	public List<Id<SignalGroup>> getOnsets(double timeSeconds) {
		Integer currentSecondInPlan = ((int) (timeSeconds  % this.cycle));
		return this.secondInPlanOnsetsMap.get(currentSecondInPlan);
	}

	
	@Override
	public double getEndTime() {
		return this.data.getEndTime();
	}

	@Override
	public double getStartTime() {
		return this.data.getStartTime();
	}

	@Override
	public Id<SignalPlan> getId() {
		return this.data.getId();
	}

	@Override
	public Integer getOffset() {
		return this.data.getOffset();
	}

	@Override
	public Integer getCycleTime() {
		return this.data.getCycleTime();
	}

	public SignalPlanData getPlanData(){
		return this.data;
	}
	
}
