/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultPlanbasedSignalSystemController
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class DefaultPlanbasedSignalSystemController implements SignalController {
	
//	private static final Logger log = Logger.getLogger(DefaultPlanbasedSignalSystemController.class);
	
	public static final String IDENTIFIER = "DefaultPlanbasedSignalSystemController";
	private Map<Id, SignalPlan> plans;
	private SignalSystem signalSystem;
	private SignalPlan activePlan;
	
	public DefaultPlanbasedSignalSystemController(){}
	
	@Override
	public void updateState(double timeSeconds) {
		this.checkActivePlan();
//		log.error("update state of system: " + this.signalSystem.getId());
		List<Id> droppingGroupIds = this.activePlan.getDroppings(timeSeconds);
		this.processDroppingGroupIds(timeSeconds, droppingGroupIds);
		
		List<Id> onsetGroupIds = this.activePlan.getOnsets(timeSeconds);
		this.processOnsetGroupIds(timeSeconds, onsetGroupIds);
	}
	
	private void processOnsetGroupIds(double timeSeconds, List<Id> onsetGroupIds) {
		if (onsetGroupIds != null){
			for (Id id : onsetGroupIds){
				this.signalSystem.scheduleOnset(timeSeconds, id);
			}
		}		
	}

	private void processDroppingGroupIds(double timeSeconds, List<Id> droppingGroupIds){
		if (droppingGroupIds != null){
			for (Id id : droppingGroupIds){
				this.signalSystem.scheduleDropping(timeSeconds, id);
			}
		}
	}
	
	private void checkActivePlan(){
		//TODO implement active plan logic
	}

	@Override
	public void simulationInitialized(double simStartTimeSeconds) {
		
	}
	
	@Override
	public void addPlan(SignalPlan plan) {
//		log.error("addPlan to system : " + this.signalSystem.getId());
		if (this.plans == null){
			this.plans = new HashMap<Id, SignalPlan>();
			//TODO remove when checkActive is implemented
			this.activePlan = plan;
		}
		this.plans.put(plan.getId(), plan);
	}

	@Override
	public void setSignalSystem(SignalSystem system) {
		this.signalSystem = system;
	}

	@Override
	public void reset(Integer iterationNumber) {
		
	}


}
