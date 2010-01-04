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
public class BasicPlanBasedSignalSystemControlInfoImpl implements  BasicPlanBasedSignalSystemControlInfo {

	private SortedMap<Id, BasicSignalSystemPlan> plans;
	
	/**
	 * @see org.matsim.signalsystems.config.BasicPlanBasedSignalSystemControlInfo#getPlans()
	 */
	public SortedMap<Id, BasicSignalSystemPlan> getPlans() {
		return plans;
	}

	/**
	 * @see org.matsim.signalsystems.config.BasicPlanBasedSignalSystemControlInfo#addPlan(org.matsim.signalsystems.config.BasicSignalSystemPlan)
	 */
	public void addPlan(BasicSignalSystemPlan plan) {
		if (this.plans == null) {
			this.plans = new TreeMap<Id, BasicSignalSystemPlan>();
		}
		this.plans.put(plan.getId(), plan);
	}

}
