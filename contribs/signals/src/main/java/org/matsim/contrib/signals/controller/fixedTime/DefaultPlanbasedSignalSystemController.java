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
package org.matsim.contrib.signals.controller.fixedTime;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.controller.AbstractSignalController;
import org.matsim.contrib.signals.controller.SignalController;
import org.matsim.contrib.signals.controller.SignalControllerFactory;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.model.SignalSystemImpl;


/**
 * Implements a Fixed-time control.
 * 
 * @author dgrether, tthunig
 *
 */
public class DefaultPlanbasedSignalSystemController extends AbstractSignalController implements SignalController {
	
	private static final Logger log = Logger.getLogger(DefaultPlanbasedSignalSystemController.class);
	
	/* an identifier 'PlanbasedSignalControl' would be better analogously to all other identifiers of signal controller
	 * but renaming would cause to much problems with old signal control input files. theresa jan'17
	 */
	public static final String IDENTIFIER = "DefaultPlanbasedSignalSystemController";
	private LinkedList<SignalPlan> planQueue = null;
	private SignalPlan activePlan = null;
	private double nextActivePlanCheckTime;
	
	public final static class FixedTimeFactory implements SignalControllerFactory {
		@Override
		public SignalController createSignalSystemController(SignalSystem signalSystem) {
			SignalController controller = new DefaultPlanbasedSignalSystemController();
			controller.setSignalSystem(signalSystem);
			return controller;
		}
	}
	
	private DefaultPlanbasedSignalSystemController() {
		super();
	}
	
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
				this.system.scheduleOnset(timeSeconds, id);
			}
		}		
	}

	private void processDroppingGroupIds(double timeSeconds, List<Id<SignalGroup>> droppingGroupIds){
		if (droppingGroupIds != null){
			for (Id<SignalGroup> id : droppingGroupIds){
				this.system.scheduleDropping(timeSeconds, id);
			}
		}
	}
	
	/**
	 * Method to be called when a check time is reached.
	 * Checks whether the next signal plan has to start or if not
	 * whether the active signal plan has to end, though.
	 * It also sets next check times.
	 * 
	 * This method may assume some important facts:
	 * 1. there is always a next plan (because activated plans are put to the end of the queue again for the next day, see startFirstPlanInQueue)
	 * 2. start and end times of signal plans always exist (see validateSignalPlans)
	 * 3. the start time of the next plan is always >= the end time of the active plan (or at the next day) (see validateSignalPlans)
	 * 
	 * @param now current time
	 */
	private void checkActivePlan(double now){
		SignalPlan nextPlan = this.planQueue.peek();
		if (nextPlan.getStartTime() == now % (3600*24)) {
			/* start time of next signal plan is reached: 
			 * stop active plan (if it is still running) and start next signal plan */
			log.info("Starting signal control plan " + nextPlan.getId());
			startFirstPlanInQueue(now);
		}
		else if (this.activePlan != null && now % (3600*24) == this.activePlan.getEndTime()) {
			/* no next plan has started but active signal plans end time is reached: 
			 * stop active signal plan. look for next starting time. */
			log.info("Switching off active signal control plan with id " + this.activePlan.getId());
//			if (nextPlan.getStartTime() > (now % (3600*24) + SignalSystemImpl.SWITCH_OFF_SEQUENCE_LENGTH)){
			double diff = nextPlan.getStartTime() - this.activePlan.getEndTime() + 24*3600;
			double mod = diff % (24*3600);
			if (mod > SignalSystemImpl.SWITCH_OFF_SEQUENCE_LENGTH){
				// switch off the signals if next plan is starting in more than 5 seconds
				this.system.switchOff(now);
			}
			waitForNextPlanInQueue(now);
		}
		else { // This case is just for completeness. It should not happen when check times are set correctly.
			/* no active plan exists and nextPlan has not started yet: determine next check time */
			waitForNextPlanInQueue(now);
		}
	}
	
	/**
	 * Correct next start or end time of a signal plan.
	 * Needed in case a plan goes over midnight or simulation reaches the next day.
	 * 
	 * @param startEndTime start or end time of a signal plan between 00:00:00 and 23:59:59 in seconds
	 * @param now current time in seconds, giving also the current day of simulation
	 * @return the next start or end time of the signal plan in seconds corresponding to the current day
	 */
	private double adaptTime2Day(double startEndTime, double now) {
		while (startEndTime <= now) {
			startEndTime += 3600 * 24.0;
		}
		return startEndTime;
	}

	@Override
	public void simulationInitialized(double simStartTime) {		
		// store all plans in a queue, sort them according to their end times and validate them
		this.planQueue = new LinkedList<SignalPlan>(this.signalPlans.values());
		Collections.sort(planQueue, new SignalPlanEndTimeComparator(simStartTime));
		validateSignalPlans();
		
		// check if first plan should already start
		SignalPlan firstPlan = this.planQueue.peek();
		if (firstPlan.getStartTime() < firstPlan.getEndTime()){
			// plan starts after midnight (i.e. does not go over midnight)
			if (firstPlan.getStartTime() <= simStartTime && firstPlan.getEndTime() >= simStartTime){
				startFirstPlanInQueue(simStartTime);
				/* possible cases:
				 * 1. simulation starts in between start and end time of the first signal plan [-|*|---|1**!*|--|**|-]
				 */
			} else { // startTime > simStartTime or endTime < simStartTime
				waitForNextPlanInQueue(simStartTime);
				/* possible cases:
				 * 1. all signal plans start and end before simStartTime [--|1**|--|**|--!------]
				 * 2. first signal plan starts after simStartTime [-|*|--!--|1**|--|**|-]
				 */
			}
		} else { // startTime >= endTime
			// plan starts before midnight (i.e. goes over midnight)
			if (firstPlan.getStartTime() <= simStartTime || firstPlan.getEndTime() >= simStartTime){
				startFirstPlanInQueue(simStartTime);
				/* possible cases:
				 * 1. midnight plan has started this day but before simulation start [**|--|*|---|1*!***]
				 * 2. midnight plan has already started last day [**!*|--|*|---|1**]
				 */
			} else { // startTime > simStartTime && endTime < simStartTime
				waitForNextPlanInQueue(simStartTime);
				/* possible cases:
				 * 1. midnight plan starts later the day [**|--|*|--!--|1***]
				 */
			}
		}
	}

	/**
	 * Activate the signal plan that is first in the queue. 
	 * Moves plan from the queues first position to the last position.
	 * (Necessary to keep simulating signals when plans go over midnight and/or the simulation lasts for more than 24h.)
	 * Determine the time when to check active plans next.
	 * 
	 * @param now current time
	 */
	private void startFirstPlanInQueue(double now) {
		this.activePlan = this.planQueue.poll();
		// shift plan to queue end
		planQueue.add(activePlan);
//		this.signalSystem.startPlan(now);
		this.nextActivePlanCheckTime = adaptTime2Day(activePlan.getEndTime(), now);
	}

	/**
	 * Reset active plan - no plan is active.
	 * Determine the time when to check active plans next.
	 * 
	 * @param now current time
	 */
	private void waitForNextPlanInQueue(double now) {
		this.activePlan = null;
		this.nextActivePlanCheckTime = adaptTime2Day(this.planQueue.peek().getStartTime(), now);
	}
	
	/**
	 * This method checks whether signal plans overlap each other.
	 * It also throws an exception if start or end times of signal plans are not specified.
	 * 
	 * @throws UnsupportedOperationException if signal plans of one signal system overlap
	 */
	private void validateSignalPlans() {
		SignalPlan planI = null;
		// iterate over all signal plans. check always overlapping between plan i and plan i+1
		for (SignalPlan planIplus1 : this.planQueue) {
			if (planI != null) {
				checkOverlapping(planI, planIplus1);
			}
			planI = planIplus1;
		}
		if (this.planQueue.size() > 1) {
			// check also overlapping of last and first plan
			checkOverlapping(planI, this.planQueue.peek());
		}
	}

	/**
	 * Assume that no plan covers the hole day.
	 * Than there are 4 valid cases how plan 1 and 2 can be ordered within the day such that they do not overlap:
	 * 1. [--|1**|--|2**|--] <=> start1 <= end1 <= start2 <= end2
	 * 2. [*|---|1**|--|2**] <=> end2 <= start1 <= end1 <= start2
	 * 3. [--|2**|---|1**|-] <=> start2 <= end2 <= start1 <= end1
	 * 4. [**|--|2**|---|1*] <=> end1 <= start2 <= end2 <= start1
	 * This means:
	 * If no plan covers the hole day it is
	 * 3 of the 4 inequalities are fulfilled <=> plans do not overlap.
	 * 
	 * The method throws an UnsupportedOperationException when less than 3 inequalities are fulfilled, i.e. when the plans overlap.
	 * 
	 * @param plan1
	 * @param plan2
	 */
	private void checkOverlapping(SignalPlan plan1, SignalPlan plan2) {
		if (plan1.getStartTime() % (24*3600) == plan1.getEndTime() % (24*3600) || plan2.getStartTime() % (24*3600) == plan2.getEndTime() % (24*3600)){
			throw new UnsupportedOperationException("Signal system " + system.getId() + " has multiple plans but at least one of them covers the hole day. "
					+ "If multiple signal plans are used, they are not allowed to overlap.");
		}
		
		int counter = 0;
		if (plan1.getStartTime() <= plan1.getEndTime())
			counter++;
		if (plan1.getEndTime() <= plan2.getStartTime())
			counter++;
		if (plan2.getStartTime() <= plan2.getEndTime())
			counter++;
		if (plan2.getEndTime() <= plan1.getStartTime())
			counter++;
		if (counter < 3) {
			throw new UnsupportedOperationException("Signal plans " + plan1.getId() + " and " + plan2.getId() + " of signal system " + system.getId() + " overlap.");
		}
	}
	
	/**
	 * Sorts Signal Plans according to their end times.
	 * The plan with the end time coming first after (not at!) simulation start gets the first position.
	 * The simulation start time is given in the constructor. 
	 */
	private static class SignalPlanEndTimeComparator implements Comparator<SignalPlan>, Serializable {

		private static final long serialVersionUID = 1L;
		private final double simStartTime;

		public SignalPlanEndTimeComparator(double simulationStart) {
			this.simStartTime = simulationStart;
		}

		@Override
		public int compare(SignalPlan p1, SignalPlan p2) {
			if (p1.getEndTime() <= simStartTime && p2.getEndTime() > simStartTime) {
				// first end time before and second after simulationStart
				return 1;
			} else if (p1.getEndTime() > simStartTime && p2.getEndTime() <= simStartTime) {
				// first end time after and second before simulationStart
				return -1;
			} else {
				// both before (incl. at) or both after simulationStart
				return Double.compare(p1.getEndTime(), p2.getEndTime());
			}
		}
		
	}

}
