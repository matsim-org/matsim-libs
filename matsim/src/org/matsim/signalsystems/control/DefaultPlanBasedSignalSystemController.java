/* *********************************************************************** *
 * project: org.matsim.*
 * PlanBasedSignalSystemControler
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.signalsystems.control;

import org.apache.log4j.Logger;
import org.matsim.core.events.SignalGroupStateChangedEventImpl;
import org.matsim.core.mobsim.queuesim.SimulationTimer;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationAfterSimStepEvent;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationInitializedEvent;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationAfterSimStepListener;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationInitializedListener;
import org.matsim.signalsystems.basic.BasicSignalGroupDefinition;
import org.matsim.signalsystems.config.BasicPlanBasedSignalSystemControlInfo;
import org.matsim.signalsystems.config.BasicSignalGroupSettings;
import org.matsim.signalsystems.config.BasicSignalSystemConfiguration;
import org.matsim.signalsystems.config.BasicSignalSystemPlan;


/**
 * Implementation of SignalSystemControler for plan controled signal groups.
 * This currently considers only the first plan and ignores plan start and stop
 * times. 
 * 
 * TODO add synchronization offset handling
 * @author dgrether
 * @author aneumann
 *
 */
public class DefaultPlanBasedSignalSystemController extends AbstractSignalSystemController implements SignalSystemController, QueueSimulationAfterSimStepListener, QueueSimulationInitializedListener {
	
	private static final Logger log = Logger
			.getLogger(DefaultPlanBasedSignalSystemController.class);
	
	private BasicSignalSystemConfiguration config;

	private BasicPlanBasedSignalSystemControlInfo plans;


	public DefaultPlanBasedSignalSystemController(BasicSignalSystemConfiguration config) {
		if (!(config.getControlInfo() instanceof BasicPlanBasedSignalSystemControlInfo)) {
			String message = "Cannot create a PlanBasedSignalSystemControler without a PlanBasedLightSignalSystemControlInfo instance!";
			log.error(message);
			throw new IllegalArgumentException(message);
		}
		this.config = config;
		this.plans = (BasicPlanBasedSignalSystemControlInfo)config.getControlInfo();
	}
	
	/**
	 * @see org.matsim.signalsystems.control.SignalSystemController#givenSignalGroupIsGreen(org.matsim.signalsystems.basic.BasicSignalGroupDefinition)
	 */
	public boolean givenSignalGroupIsGreen(double time, 
			BasicSignalGroupDefinition signalGroup) {
		SignalGroupState state = this.getSignalGroupStates().get(signalGroup);
		if (state.equals(SignalGroupState.GREEN) || state.equals(SignalGroupState.REDYELLOW)){
			return true;
		}
		return false;
	}
	
	
	private void updateSignalGroupStates(double time) {
		BasicSignalSystemPlan activePlan = this.plans.getPlans().values().iterator().next();
		if (activePlan == null) {
			String message = "No active plan for signalsystem id " + config.getSignalSystemId();
			log.error(message);
			throw new IllegalStateException(message);
		}
		double cycleTime;
		if (activePlan.getCycleTime() != null){
			cycleTime = activePlan.getCycleTime();
		}
		else if (this.getDefaultCycleTime() != null){
			cycleTime = this.getDefaultCycleTime();
		}
		else {
			throw new IllegalStateException("CycleTime is not set for SignalSystemConfiguration of SignalSystem Id:  " + this.config.getSignalSystemId());
		}
		//TODO remove 1 + and set therefore <= comparison
		int currentSecondInPlan = 1 + ((int) (time % cycleTime));

		int roughcast, dropping, endIntergreenRc, endIntergreenDrop;
		SignalGroupState currentState, newState;
		for (BasicSignalGroupDefinition g : this.getSignalGroups().values()){
			BasicSignalGroupSettings sgc = activePlan.getGroupConfigs().get(g.getId());
			currentState = this.getSignalGroupStates().get(g);
			roughcast = sgc.getRoughCast();
			dropping = sgc.getDropping();
			endIntergreenRc = roughcast + sgc.getInterimGreenTimeRoughcast();
			endIntergreenDrop = dropping + sgc.getInterGreenTimeDropping();
			if ((roughcast < currentSecondInPlan) && 
					(currentSecondInPlan <=  endIntergreenRc)) {
				newState = SignalGroupState.REDYELLOW;
			}
			else if ((endIntergreenRc < currentSecondInPlan) && (currentSecondInPlan <= dropping)) {
				newState = SignalGroupState.GREEN;
			}
			else if ((dropping < currentSecondInPlan) && (currentSecondInPlan <= endIntergreenDrop)) {
				newState = SignalGroupState.YELLOW;
			}
			else {
				newState = SignalGroupState.RED;
			}
			this.getSignalGroupStates().put(g, newState);
			if (newState != currentState){
				this.getSignalEngine().getEvents().processEvent(new SignalGroupStateChangedEventImpl(time, g.getSignalSystemDefinitionId(), g.getId(), newState));
			}
		}
	}

	public void notifySimulationAfterSimStep(QueueSimulationAfterSimStepEvent e) { 
		//+1 to update the green state for the next timestep
		this.updateSignalGroupStates(e.getSimulationTime() + 1.0);
	}

	public void notifySimulationInitialized(QueueSimulationInitializedEvent e) {
		this.updateSignalGroupStates(SimulationTimer.getSimStartTime());
	}
}
