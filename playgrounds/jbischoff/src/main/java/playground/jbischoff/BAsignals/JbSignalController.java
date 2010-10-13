/* *********************************************************************** *
 * project: org.matsim.*
 * JbSignalController
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
package playground.jbischoff.BAsignals;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.SignalGroupStateChangedEventImpl;
import org.matsim.signalsystems.config.PlanBasedSignalSystemControlInfo;
import org.matsim.signalsystems.config.SignalGroupSettings;
import org.matsim.signalsystems.config.SignalSystemConfiguration;
import org.matsim.signalsystems.config.SignalSystemPlan;
import org.matsim.signalsystems.control.AbstractSignalSystemController;
import org.matsim.signalsystems.control.DefaultPlanBasedSignalSystemController;
import org.matsim.signalsystems.control.SignalGroupState;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signalsystems.model.SignalController;
import org.matsim.signalsystems.model.SignalPlan;
import org.matsim.signalsystems.model.SignalSystem;
import org.matsim.signalsystems.systems.SignalGroupDefinition;


/**
 * @author dgrether
 *
 */
public class JbSignalController extends AbstractSignalSystemController implements SignalController {

	private static final Logger log = Logger.getLogger(JbSignalController.class);
	private SignalSystemConfiguration config;
	private PlanBasedSignalSystemControlInfo plans;
	private Id id1 = new IdImpl(1);
	private SignalSystem system;
	private AdaptiveControllHead adaptiveControllHead;
	private Map<Id,SignalPlan> spd;
	
	public JbSignalController() {
		spd = new HashMap<Id,SignalPlan>();
		
	/*	if (!(config.getControlInfo() instanceof PlanBasedSignalSystemControlInfo)) {
			String message = "Cannot create a PlanBasedSignalSystemControler without a PlanBasedLightSignalSystemControlInfo instance!";
			log.error(message);
			throw new IllegalArgumentException(message);
		}*/
		this.config = config;
		this.plans = (PlanBasedSignalSystemControlInfo)config.getControlInfo();
		
	
	}
	@Override
	public void addPlan(SignalPlan plan) {
		this.spd.put(plan.getId(), plan);
	}

	@Override
	public void setSignalSystem(SignalSystem system) {
		this.system = system;
		
		
	}

	@Override
	public void updateState(double timeSeconds){
	SignalsData signalData = this.system.getSignalSystemsManager().getSignalsData();
	SignalPlan activePlan = this.spd.get(this.getCurrentPlanId(timeSeconds));
	double cycleTime;
	if (signalData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(this.system.getId()).getSignalPlanData().get(activePlan.getId()).getCycleTime() != null){
		cycleTime = signalData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(this.system.getId()).getSignalPlanData().get(activePlan.getId()).getCycleTime();
	}
	else if (this.getDefaultCycleTime() != null){
		cycleTime = this.getDefaultCycleTime();
	}
	else {
		throw new IllegalStateException("CycleTime is not set for SignalSystemConfiguration of SignalSystem Id:  " + this.config.getSignalSystemId());
	}
	int currentSecondInPlan = ((int) (timeSeconds % cycleTime));
	int roughcast, dropping, endIntergreenRc, endIntergreenDrop;
	SignalGroupState currentState, newState;
	
	
	
}
	
	private Id getCurrentPlanId(double timeSeconds){
		return this.id1;
		//construction site, so far only one plan is considered
	}


	@Override
	public void reset(Integer iterationNumber) {
		// TODO Auto-generated method stub
		
	}
	//@Override
	public SignalGroupState getSignalGroupState(double seconds,
			SignalGroupDefinition signalGroup) {
	    return this.getSignalGroupStates().get(signalGroup);

	}

}
