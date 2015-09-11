/* *********************************************************************** *
 * project: org.matsim.*
 * Person.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.core.population;

import org.matsim.api.core.v01.Customizable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.CustomizableUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
/**
 * Default implementation of {@link Person} interface.
 */
public final class PersonImpl implements Person {

	protected List<Plan> plans = new ArrayList<Plan>(6);
	protected Id<Person> id;

	private TreeSet<String> travelcards = null;

	private Plan selectedPlan = null;

	private Customizable customizableDelegate;

	@Deprecated // please try to use the factory: pop.getFactory().create...
	 PersonImpl(final Id<Person> id) {
		this.id = id;
	}

	public static Person createPerson(final Id<Person> id) {
		return new PersonImpl(id);
	}

	@Override
	public final Plan getSelectedPlan() {
		return this.selectedPlan;
	}

	@Override
	public boolean addPlan(final Plan plan) {
		plan.setPerson(this);
		// Make sure there is a selected plan if there is at least one plan
		if (this.selectedPlan == null) this.selectedPlan = plan;
		return this.plans.add(plan);
	}

	@Override
	public final void setSelectedPlan(final Plan selectedPlan) {
		if (selectedPlan != null && !plans.contains( selectedPlan )) {
			throw new IllegalStateException("The plan to be set as selected is not null nor stored in the person's plans");
		}
		this.selectedPlan = selectedPlan;
	}

	@Override
	public Plan createCopyOfSelectedPlanAndMakeSelected() {
		Plan oldPlan = this.getSelectedPlan();
		if (oldPlan == null) {
			return null;
		}
		PlanImpl newPlan = new PlanImpl(oldPlan.getPerson());
		newPlan.copyFrom(oldPlan);
		this.getPlans().add(newPlan);
		this.setSelectedPlan(newPlan);
		return newPlan;
	}

	@Override
	public Id<Person> getId() {
		return this.id;
	}

    // Not on interface. Only to be used for demand generation.
	public void setId(final Id<Person> id) {
		this.id = id;
	}

	@Override
	public final String toString() {
		StringBuilder b = new StringBuilder();
		b.append("[id=").append(this.getId()).append("]");
		b.append("[nof_plans=").append(this.getPlans() == null ? "null" : this.getPlans().size()).append("]");
		return b.toString();
	}

	@Override
	public boolean removePlan(final Plan plan) {
		boolean result = this.getPlans().remove(plan);
		if ((this.getSelectedPlan() == plan) && result) {
			this.setSelectedPlan(new RandomPlanSelector<Plan, Person>().selectPlan(this));
		}
		return result;
	}

	@Override
	public List<Plan> getPlans() {
		return this.plans;
	}


	@Override
	public Map<String, Object> getCustomAttributes() {
		if (this.customizableDelegate == null) {
			this.customizableDelegate = CustomizableUtils.createCustomizable();
		}
		return this.customizableDelegate.getCustomAttributes();
	}

}
