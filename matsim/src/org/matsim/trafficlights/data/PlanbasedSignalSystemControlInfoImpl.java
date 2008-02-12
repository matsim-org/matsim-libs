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
package org.matsim.trafficlights.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



/**
 * @author dgrether
 *
 */
public class PlanbasedSignalSystemControlInfoImpl implements PlanbasedSignalSystemControlInfo {

	private List<SignalSystemPlan> signalPlans  = new ArrayList<SignalSystemPlan>();

	private Set<SignalGroupDefinition> signalGroupDefinitions = new HashSet<SignalGroupDefinition>();

	public PlanbasedSignalSystemControlInfoImpl() {
	}

	public List<SignalSystemPlan> getSignalSystemPlans() {
		return this.signalPlans;
	}

	public void addSignalSystemPlan(SignalSystemPlan signalPlan) {
		this.signalPlans.add(signalPlan);
		this.signalGroupDefinitions.addAll(signalPlan.getSignalGroupDefinitions());
	}

	public Set<SignalGroupDefinition> getSignalGroupDefinitions() {
		return this.signalGroupDefinitions;
	}
}
