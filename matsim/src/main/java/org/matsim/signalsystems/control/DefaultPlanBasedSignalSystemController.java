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
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.ptproject.qsim.QSimTimerStatic;
import org.matsim.signalsystems.config.PlanBasedSignalSystemControlInfo;
import org.matsim.signalsystems.config.SignalGroupSettings;
import org.matsim.signalsystems.config.SignalSystemConfiguration;
import org.matsim.signalsystems.config.SignalSystemPlan;
import org.matsim.signalsystems.systems.SignalGroupDefinition;


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
public class DefaultPlanBasedSignalSystemController extends AbstractSignalSystemController implements SignalSystemController, 
  SimulationInitializedListener, SimulationBeforeSimStepListener {
	
	private static final Logger log = Logger
			.getLogger(DefaultPlanBasedSignalSystemController.class);
	
	private SignalSystemConfiguration config;

	private PlanBasedSignalSystemControlInfo plans;


	public DefaultPlanBasedSignalSystemController(SignalSystemConfiguration config) {
		if (!(config.getControlInfo() instanceof PlanBasedSignalSystemControlInfo)) {
			String message = "Cannot create a PlanBasedSignalSystemControler without a PlanBasedLightSignalSystemControlInfo instance!";
			log.error(message);
			throw new IllegalArgumentException(message);
		}
		this.config = config;
		this.plans = (PlanBasedSignalSystemControlInfo)config.getControlInfo();
	}
	
	
  @Override
  public SignalGroupState getSignalGroupState(double seconds,
      SignalGroupDefinition signalGroup) {
    return this.getSignalGroupStates().get(signalGroup);
  }
	
  @Override
  public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
    this.updateState(e.getSimulationTime());
  }
  
  private void updateState(double time) {
		SignalSystemPlan activePlan = this.plans.getPlans().values().iterator().next();
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
		int currentSecondInPlan = ((int) (time % cycleTime));

		int roughcast, dropping, endIntergreenRc, endIntergreenDrop;
		SignalGroupState currentState, newState;
		for (SignalGroupDefinition g : this.getSignalGroups().values()){
			SignalGroupSettings sgc = activePlan.getGroupConfigs().get(g.getId());
			currentState = this.getSignalGroupStates().get(g);
			roughcast = sgc.getRoughCast();
			dropping = sgc.getDropping();
			endIntergreenRc = roughcast + sgc.getInterimGreenTimeRoughcast();
			endIntergreenDrop = dropping + sgc.getInterGreenTimeDropping();
			if(roughcast <= dropping){
				if ((roughcast <= currentSecondInPlan) &&
						(currentSecondInPlan <  endIntergreenRc)) {
					newState = SignalGroupState.REDYELLOW;
				}
				else if ((endIntergreenRc <= currentSecondInPlan) && 
						(currentSecondInPlan < dropping)) {
					newState = SignalGroupState.GREEN;
				}
				else if (((dropping <= currentSecondInPlan) && (currentSecondInPlan < endIntergreenDrop)) ||
						((endIntergreenDrop > cycleTime) && (currentSecondInPlan < (endIntergreenDrop - cycleTime)))){
					newState = SignalGroupState.YELLOW;
				}
				else {
					newState = SignalGroupState.RED;
				}
			} else {
				if (((roughcast <= currentSecondInPlan) && (currentSecondInPlan <  endIntergreenRc)) ||
						((endIntergreenRc > cycleTime) && (currentSecondInPlan < (endIntergreenRc - cycleTime)))){
					newState = SignalGroupState.REDYELLOW;
				}
				else if ((endIntergreenDrop <= currentSecondInPlan) && 
						(currentSecondInPlan < roughcast)) {
					newState = SignalGroupState.RED;
				}
				else if ((dropping <= currentSecondInPlan) && (currentSecondInPlan < 
						endIntergreenDrop)) {
					newState = SignalGroupState.YELLOW;
				}
				else {
					newState = SignalGroupState.GREEN;
				}
			}
			this.getSignalGroupStates().put(g, newState);
			if (newState != currentState){
				this.getSignalEngine().getEvents().processEvent(new SignalGroupStateChangedEventImpl(time, g.getSignalSystemDefinitionId(), g.getId(), newState));
			}
		}
	}


	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		this.updateState(QSimTimerStatic.getSimStartTime());
	}

}
