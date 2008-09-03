/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.basic.lightsignalsystemsconfig;

import java.util.HashMap;
import java.util.Map;

import org.matsim.basic.v01.Id;


/**
 * @author dgrether
 *
 */
public class BasicPlanBasedLightSignalSystemControlInfo implements BasicLightSignalSystemControlInfo {

	private Map<Id, BasicLightSignalSystemPlan> plans;

	
	public Map<Id, BasicLightSignalSystemPlan> getPlans() {
		return plans;
	}

	public void addPlan(BasicLightSignalSystemPlan plan) {
		if (this.plans == null) {
			this.plans = new HashMap<Id, BasicLightSignalSystemPlan>();
		}
		this.plans.put(plan.getId(), plan);
	}

}
