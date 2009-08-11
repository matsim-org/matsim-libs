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
import org.matsim.signalsystems.basic.BasicSignalGroupDefinition;
import org.matsim.signalsystems.config.BasicPlanBasedSignalSystemControlInfo;
import org.matsim.signalsystems.config.BasicSignalGroupSettings;
import org.matsim.signalsystems.config.BasicSignalSystemConfiguration;
import org.matsim.signalsystems.config.BasicSignalSystemPlan;


/**
 * Implementation of SignalSystemControler for plan controled signal groups.
 * This currently considers only the first plan and ignores plan start and stop
 * times. Furthermore it ignores intergreen times.
 * 
 * TODO add synchronization offset handling
 * @author dgrether
 * @author aneumann
 *
 */
public class DefaultPlanBasedSignalSystemController extends AbstractSignalSystemController implements SignalSystemController {
	
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
		int currentSecondInPlan = 1 + ((int) (time % cycleTime));

		BasicSignalGroupSettings signalGroupConfig = activePlan.getGroupConfigs().get(signalGroup.getId());
		if ( (signalGroupConfig.getRoughCast() < currentSecondInPlan) 
				&& (currentSecondInPlan <= signalGroupConfig.getDropping())){
			return true;
		}
		return false;
	}

}
