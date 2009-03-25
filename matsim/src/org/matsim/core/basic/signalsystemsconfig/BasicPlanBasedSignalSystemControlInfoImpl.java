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

package org.matsim.core.basic.signalsystemsconfig;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.basic.v01.Id;

/**
 * @author dgrether
 */
public class BasicPlanBasedSignalSystemControlInfoImpl implements  BasicPlanBasedSignalSystemControlInfo {

	private Map<Id, BasicSignalSystemPlan> plans;
	
	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicPlanBasedSignalSystemControlInfo#getPlans()
	 */
	public Map<Id, BasicSignalSystemPlan> getPlans() {
		return plans;
	}

	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicPlanBasedSignalSystemControlInfo#addPlan(org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemPlan)
	 */
	public void addPlan(BasicSignalSystemPlan plan) {
		if (this.plans == null) {
			this.plans = new HashMap<Id, BasicSignalSystemPlan>();
		}
		this.plans.put(plan.getId(), plan);
	}

}
