/* *********************************************************************** *
 * project: org.matsim.*
 * RawPersonImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.plans.plain.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;

import playground.johannes.plans.plain.PlainPerson;
import playground.johannes.plans.plain.PlainPlan;

/**
 * @author illenberger
 *
 */
public class PlainPersonImpl extends AbstractModifiable implements PlainPerson {

	private final Id id;
	
	private ArrayList<PlainPlanImpl> plans;
	
	private List<PlainPlanImpl> unmodifiablePlans;
	
	private PlainPlanImpl selectedPlan;
	
	public PlainPersonImpl(Id id) {
		this.id = id;
		plans = new ArrayList<PlainPlanImpl>();
		unmodifiablePlans = Collections.unmodifiableList(plans);
	}
	
	public List<? extends PlainPlanImpl> getPlans() {
		return unmodifiablePlans;
	}

	public void addPlan(PlainPlan plan) {
		plans.add((PlainPlanImpl) plan);
		plans.trimToSize();
		modified();
	}

	public Id getId() {
		return id;
	}

	public PlainPlan getSelectedPlan() {
		return selectedPlan;
	}

	public void removePlan(PlainPlan plan) {
		plans.remove(plan);
		modified();
	}

	public void setSelectedPlan(PlainPlan plan) {
		if(plans.contains(plan)) {
			selectedPlan = (PlainPlanImpl) plan;
			modified();
		} else
			throw new IllegalArgumentException("Plan is not part of the person's plan list. Add the plan to the person before!");
	}

}
