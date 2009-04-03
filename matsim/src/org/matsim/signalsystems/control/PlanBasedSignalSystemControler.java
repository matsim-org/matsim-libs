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
import org.matsim.core.basic.signalsystems.BasicSignalGroupDefinition;
import org.matsim.core.basic.signalsystemsconfig.BasicPlanBasedSignalSystemControlInfo;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalGroupSettings;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemConfiguration;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemPlan;
import org.matsim.core.mobsim.queuesim.SimulationTimer;


/**
 * Implementation of SignalSystemControler for plan controled signal groups.
 * TODO check default and plan based circulation time
 * TODO check plan activation and deactivation
 * TODO check abstract class: interface would be a nicer solution
 * TODO reconsider interface: extend by giving the current second as an argument
 * @author dgrether
 * @author aneumann
 *
 */
public class PlanBasedSignalSystemControler implements SignalSystemControler {
	
	private static final Logger log = Logger
			.getLogger(PlanBasedSignalSystemControler.class);
	
	private double defaultCirculationTime = Double.NaN;
	private BasicSignalSystemConfiguration config;

	private BasicPlanBasedSignalSystemControlInfo plans;


	public PlanBasedSignalSystemControler(BasicSignalSystemConfiguration config) {
		if (!(config.getControlInfo() instanceof BasicPlanBasedSignalSystemControlInfo)) {
			String message = "Cannot create a PlanBasedSignalSystemControler without a PlanBasedLightSignalSystemControlInfo instance!";
			log.error(message);
			throw new IllegalArgumentException(message);
		}
		this.config = config;
		this.plans = (BasicPlanBasedSignalSystemControlInfo)config.getControlInfo();
	}
	
	
	public void setDefaultCirculationTime(double circulationTime) {
		this.defaultCirculationTime = circulationTime ;
	}
	
	
	/**
	 * TODO include time argument to avoid static call to SimulationTimer.getTime()
	 * @see org.matsim.signalsystems.control.SignalSystemControler#givenSignalGroupIsGreen(org.matsim.core.basic.signalsystems.BasicSignalGroupDefinition)
	 */
	public boolean givenSignalGroupIsGreen(
			BasicSignalGroupDefinition signalGroup) {
		BasicSignalSystemPlan activePlan = this.plans.getPlans().values().iterator().next();
		if (activePlan == null) {
			String message = "No active plan for signalsystem id " + config.getSignalSystemId();
			log.error(message);
			throw new IllegalStateException(message);
		}
		double circulationTime;
		if (activePlan.getCycleTime() != null){
			circulationTime = activePlan.getCycleTime();
		}
		else {
			circulationTime = this.defaultCirculationTime;
		}
		int currentSecondInPlan = 1 + ((int) (SimulationTimer.getTime() % circulationTime));

		BasicSignalGroupSettings signalGroupConfig = activePlan.getGroupConfigs().get(signalGroup.getId());
		if ( (signalGroupConfig.getRoughCast() < currentSecondInPlan) 
				&& (currentSecondInPlan <= signalGroupConfig.getDropping())){
			return true;
		}
		return false;
	}

}
