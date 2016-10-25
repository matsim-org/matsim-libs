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
package org.matsim.contrib.signals.model;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;


/**
 * Implements a Fixed-time control.
 * 
 * @author dgrether
 *
 */
public class DefaultPlanbasedSignalSystemController implements SignalController {
	
	private static final Logger log = Logger.getLogger(DefaultPlanbasedSignalSystemController.class);
	
	public static final String IDENTIFIER = "DefaultPlanbasedSignalSystemController";
	private Queue<SignalPlan> planQueue = null;
	private Map<Id<SignalPlan>, SignalPlan> plans = null;
	private SignalSystem signalSystem = null;
	private SignalPlan activePlan = null;
	private double nextActivePlanCheckTime;
	
	public DefaultPlanbasedSignalSystemController(){}
	
	@Override
	public void updateState(double timeSeconds) {
		if (nextActivePlanCheckTime <= timeSeconds){
			this.checkActivePlan(timeSeconds);
		}
		if (this.activePlan != null){
			// process droppings and onsets of the active plan of this second
			List<Id<SignalGroup>> droppingGroupIds = this.activePlan.getDroppings(timeSeconds);
			this.processDroppingGroupIds(timeSeconds, droppingGroupIds);
			
			List<Id<SignalGroup>> onsetGroupIds = this.activePlan.getOnsets(timeSeconds);
			this.processOnsetGroupIds(timeSeconds, onsetGroupIds);
		}
	}
	
	private void processOnsetGroupIds(double timeSeconds, List<Id<SignalGroup>> onsetGroupIds) {
		if (onsetGroupIds != null){
			for (Id<SignalGroup> id : onsetGroupIds){
				this.signalSystem.scheduleOnset(timeSeconds, id);
			}
		}		
	}

	private void processDroppingGroupIds(double timeSeconds, List<Id<SignalGroup>> droppingGroupIds){
		if (droppingGroupIds != null){
			for (Id<SignalGroup> id : droppingGroupIds){
				this.signalSystem.scheduleDropping(timeSeconds, id);
			}
		}
	}
	
	private void checkActivePlan(double timeSeconds){
		SignalPlan nextPlan = this.planQueue.peek();
		if (nextPlan != null && nextPlan.getStartTime() != null 
				&& nextPlan.getStartTime() <= timeSeconds) {
			/* start time of next signal plan is reached: 
			 * stop active plan (if it is still running) and start next signal plan */
			log.info("Disabling active signal control plan. Switching to control plan " + nextPlan.getId());
			this.activePlan = nextPlan;
			this.planQueue.poll();
			
			//determine next check of active plan
			Double activeEndTime = this.activePlan.getEndTime();
			if (activeEndTime != null && activeEndTime.equals(0.0)){
				// correct end time if 0am is given instead of 24pm
				activeEndTime = 3600 * 24.0;
			}
			nextPlan = this.planQueue.peek();
			if (nextPlan != null){
				if (activeEndTime == null && nextPlan.getStartTime() != null) {
					this.nextActivePlanCheckTime = nextPlan.getStartTime();
				}
				else if (activeEndTime != null  && nextPlan.getStartTime() == null){
					this.nextActivePlanCheckTime = activeEndTime;
				}
				else {
					this.nextActivePlanCheckTime = Math.min(activeEndTime, nextPlan.getStartTime());
				}
			}
			//no next plan
			else if (activeEndTime != null){
				this.nextActivePlanCheckTime = activeEndTime;
			}
			else {
				this.nextActivePlanCheckTime = Double.POSITIVE_INFINITY;
			}
		}
		else if (this.activePlan != null && this.activePlan.getEndTime() != null 
				&&  timeSeconds >= this.activePlan.getEndTime()) {
			/* no next plan has started but active signal plans end time is reached: 
			 * stop active signal plan. look for next starting time. */
			this.activePlan = null;
			if (nextPlan != null && nextPlan.getStartTime() != null 
					&& nextPlan.getStartTime() <= (timeSeconds + SignalSystemImpl.SWITCH_OFF_SEQUENCE_LENGTH)){
				this.nextActivePlanCheckTime = nextPlan.getStartTime();
			}
			else {
				this.signalSystem.switchOff(timeSeconds);
			}
		}
	}

	@Override
	public void simulationInitialized(double simStartTimeSeconds) {
		this.planQueue = new PriorityBlockingQueue<SignalPlan>(this.plans.size(), new SignalPlanStartTimeComparator());
		this.planQueue.addAll(this.plans.values());
		//first check if there is a plan that shall be active all the time (i.e. 0.0 as start and end time)
		this.activePlan = this.planQueue.peek();
		if ((this.activePlan.getStartTime() == null || this.activePlan.getStartTime() == 0.0) 
				&& (this.activePlan.getEndTime() == null || this.activePlan.getEndTime() == 0.0)){
			this.nextActivePlanCheckTime = Double.POSITIVE_INFINITY;
			this.planQueue = null;
		}
		else {
			this.checkActivePlan(simStartTimeSeconds);
		}
	}
	
	@Override
	public void addPlan(SignalPlan plan) {
//		log.error("addPlan to system : " + this.signalSystem.getId());
		if (this.plans == null){
			this.plans = new HashMap<>();
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

	
	private static class SignalPlanStartTimeComparator implements Comparator<SignalPlan>, Serializable {

		private static final long serialVersionUID = 1L;

		@Override
		public int compare(SignalPlan p1, SignalPlan p2) {
			if (p1.getStartTime() != null && p2.getStartTime() != null) {
				return p1.getStartTime().compareTo(p2.getStartTime());
			}
			else if (p1.getStartTime() != null && p2.getStartTime() == null){
				return -1;
			}
			else if (p1.getStartTime() == null && p2.getStartTime() != null){
				return 1;
			}
			else {
				return 0;
			}
		}
		
	}

}
