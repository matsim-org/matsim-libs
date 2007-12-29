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

import org.matsim.utils.identifiers.IdI;

public class BasicPerson<T extends BasicPlan> {

	protected List<T> plans = new ArrayList<T>(6);
	protected T selectedPlan = null;
	protected IdI id;

	public BasicPerson(final String id) {
		this(new Id(id));
	}

	public BasicPerson(final Id id) {
		this.id = id;
	}

	public void addPlan(final T plan) {
		this.plans.add(plan);
		// Make sure there is a selected plan if there is at least one plan
		if (this.selectedPlan == null) this.selectedPlan = plan;
	}

	public T getSelectedPlan() {
		return this.selectedPlan;
	}

	/**
	 * Sets the selected plan of a person. If the plan is not part of the person,
	 * nothing is changed.
	 *
	 * @param selectedPlan the plan to be the selected one of the person
	 */
	public void setSelectedPlan(final T selectedPlan) {
		if (this.plans.contains(selectedPlan)) {
			this.selectedPlan = selectedPlan;
		}
	}

	public List<T> getPlans() {
		return this.plans;
	}

	public IdI getId() {
		return this.id;
	}

	public void setId(final IdI id) {
		this.id = id;
	}

	public void setId(final String idstring) {
		this.id = new Id(idstring);
	}

}
