/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.signalsystems.config;

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

/**
 * @author dgrether
 */
public class PlanBasedSignalSystemControlInfoImpl implements  PlanBasedSignalSystemControlInfo {

	private SortedMap<Id, SignalSystemPlan> plans;
	
	/**
	 * @see org.matsim.signalsystems.config.PlanBasedSignalSystemControlInfo#getPlans()
	 */
	public SortedMap<Id, SignalSystemPlan> getPlans() {
		return plans;
	}

	/**
	 * @see org.matsim.signalsystems.config.PlanBasedSignalSystemControlInfo#addPlan(org.matsim.signalsystems.config.SignalSystemPlan)
	 */
	public void addPlan(SignalSystemPlan plan) {
		if (this.plans == null) {
			this.plans = new TreeMap<Id, SignalSystemPlan>();
		}
		this.plans.put(plan.getId(), plan);
	}

}
