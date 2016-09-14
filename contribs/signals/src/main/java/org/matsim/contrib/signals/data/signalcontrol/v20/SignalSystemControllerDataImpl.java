/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemControllerDataImpl
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
package org.matsim.contrib.signals.data.signalcontrol.v20;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;


/**
 * @author dgrether
 *
 */
public class SignalSystemControllerDataImpl implements SignalSystemControllerData {

	private String controllerIdentifier;
	private Id<SignalSystem> signalSystemId;
	private Map<Id<SignalPlan>, SignalPlanData> signalPlanData;

	public SignalSystemControllerDataImpl(Id<SignalSystem> signalSystemId) {
		this.signalSystemId = signalSystemId;
	}

	@Override
	public void addSignalPlanData(SignalPlanData plan) {
		if (this.signalPlanData == null){
			this.signalPlanData = new TreeMap<>();
		}
		this.signalPlanData.put(plan.getId(), plan);
	}

	@Override
	public String getControllerIdentifier() {
		return this.controllerIdentifier;
	}

	@Override
	public Id<SignalSystem> getSignalSystemId() {
		return this.signalSystemId;
	}

	@Override
	public Map<Id<SignalPlan>, SignalPlanData> getSignalPlanData() {
		return this.signalPlanData;
	}

	@Override
	public void setControllerIdentifier(String identifier) {
		this.controllerIdentifier = identifier;
	}

}
