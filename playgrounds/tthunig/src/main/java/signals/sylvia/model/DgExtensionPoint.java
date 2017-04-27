/* *********************************************************************** *
 * project: org.matsim.*
 * DgExtensionPoint
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package signals.sylvia.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;


/**
 * Has no idea about the sensors as it should be a datastructure
 * but might have an idea about the links of the signal groups
 * @author dgrether
 *
 */
public class DgExtensionPoint {

	private Map<Id<SignalGroup>, Integer> maxGreenTimes = new HashMap<>();
	private int secondInPlan;
	private Set<Id<SignalGroup>> signalGroupIds = new HashSet<>();
	private Set<Signal> signals = new HashSet<>();

	public DgExtensionPoint(int secondInPlan){
		this.secondInPlan = secondInPlan;
	}
	
	public int getSecondInPlan(){
		return this.secondInPlan;
	}
	
	public Set<Id<SignalGroup>> getSignalGroupIds() {
		return this.signalGroupIds ;
	}
	
	public void addSignalGroupId(Id<SignalGroup> signalGroupId){
		this.signalGroupIds.add(signalGroupId);
	}

	public int getMaxGreenTime(Id<SignalGroup> signalGroupId) {
		return this.maxGreenTimes.get(signalGroupId);
	}
	
	public void setMaxGreenTime(Id<SignalGroup> signalGroupId, int maxGreenTime){
		this.maxGreenTimes.put(signalGroupId, maxGreenTime);
	}

	public void addSignals(Set<Signal> signals) {
		this.signals.addAll(signals);
	}
	
	public Set<Signal> getSignals(){
		return this.signals;
	}

}
