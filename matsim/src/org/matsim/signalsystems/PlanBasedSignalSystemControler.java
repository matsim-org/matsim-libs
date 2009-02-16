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
package org.matsim.signalsystems;

import org.apache.log4j.Logger;
import org.matsim.basic.signalsystems.BasicLightSignalGroupDefinition;
import org.matsim.basic.signalsystems.control.SignalSystemControler;
import org.matsim.basic.signalsystemsconfig.BasicLightSignalGroupConfiguration;
import org.matsim.basic.signalsystemsconfig.BasicLightSignalSystemConfiguration;
import org.matsim.basic.signalsystemsconfig.BasicLightSignalSystemPlan;
import org.matsim.basic.signalsystemsconfig.BasicPlanBasedLightSignalSystemControlInfo;
import org.matsim.mobsim.queuesim.SimulationTimer;


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
public class PlanBasedSignalSystemControler extends SignalSystemControler {
	
	private static final Logger log = Logger
			.getLogger(PlanBasedSignalSystemControler.class);
	
	private double defaultCirculationTime = Double.NaN;
	private BasicLightSignalSystemConfiguration config;

	private BasicPlanBasedLightSignalSystemControlInfo plans;


	public PlanBasedSignalSystemControler(BasicLightSignalSystemConfiguration config) {
		if (!(config.getControlInfo() instanceof BasicPlanBasedLightSignalSystemControlInfo)) {
			String message = "Cannot create a PlanBasedSignalSystemControler without a PlanBasedLightSignalSystemControlInfo instance!";
			log.error(message);
			throw new IllegalArgumentException(message);
		}
		this.config = config;
		this.plans = (BasicPlanBasedLightSignalSystemControlInfo)config.getControlInfo();
	}
	
	
	public void setDefaultCirculationTime(double circulationTime) {
		this.defaultCirculationTime = circulationTime ;
	}
	
	
	/**
	 * @see org.matsim.basic.signalsystems.control.SignalSystemControler#givenSignalGroupIsGreen(org.matsim.basic.signalsystems.BasicLightSignalGroupDefinition)
	 */
	@Override
	public boolean givenSignalGroupIsGreen(
			BasicLightSignalGroupDefinition signalGroup) {
		BasicLightSignalSystemPlan activePlan = this.plans.getPlans().values().iterator().next();
		if (activePlan == null) {
			String message = "No active plan for signalsystem id " + config.getLightSignalSystemId();
			log.error(message);
			throw new IllegalStateException(message);
		}
		int currentSecondInPlan = 1 + ((int) (SimulationTimer.getTime() % this.defaultCirculationTime));

		BasicLightSignalGroupConfiguration signalGroupConfig = activePlan.getGroupConfigs().get(signalGroup.getId());
		if ( (signalGroupConfig.getRoughCast() < currentSecondInPlan) 
				&& (currentSecondInPlan <= signalGroupConfig.getDropping())){
			return true;
		}
		return false;
	}

}
