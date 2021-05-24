/* *********************************************************************** *
 * project: org.matsim.*
 * DgAbstractSignalController
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.controller;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;

/**
 * 
 * @author dgrether
 *
 */
public abstract class AbstractSignalController implements SignalController {

	protected SignalSystem system ;
	protected Map<Id<SignalPlan>, SignalPlan> signalPlans = new HashMap<>();

	@Override
	public void addPlan(SignalPlan plan) {
		this.signalPlans.put(plan.getId(), plan);
	}

	@Override
	public void setSignalSystem(SignalSystem signalSystem) {
		this.system = signalSystem;
	}
}
