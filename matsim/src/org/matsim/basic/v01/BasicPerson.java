/* *********************************************************************** *
 * project: org.matsim.*
 * BasicPerson.java
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

package org.matsim.basic.v01;

import java.util.ArrayList;
import java.util.List;



public class BasicPerson<T extends BasicPlan> extends BasicIdentified{

	protected List<T> plans = new ArrayList<T>(6);
	protected T selectedPlan = null;

	public BasicPerson(String id) {
		super(new Id(id));
	}

	public BasicPerson(Id id) {
		super(id);
	}

	public void addPlan(T plan) {
		plans.add(plan);
		// Make sure there is a selected plan if there is at least one plan
		if (selectedPlan == null) selectedPlan = plan;
	}

	public T getSelectedPlan() {
		return selectedPlan;
	}

	/**
	 * Sets the selected plan of a person. If the plan is not part of the person,
	 * nothing is changed.
	 *
	 * @param selectedPlan the plan to be the selected one of the person
	 */
	public void setSelectedPlan(T selectedPlan) {
		if (this.plans.contains(selectedPlan)) {
			this.selectedPlan = selectedPlan;
		} else {
			// Do nothing
		}
	}

	public List<T> getPlans() {
		return plans;
	}

}
