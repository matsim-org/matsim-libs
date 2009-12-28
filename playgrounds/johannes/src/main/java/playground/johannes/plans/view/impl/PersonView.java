/* *********************************************************************** *
 * project: org.matsim.*
 * PersonImpl.java
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
package playground.johannes.plans.view.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;

import playground.johannes.plans.plain.impl.PlainPersonImpl;
import playground.johannes.plans.plain.impl.PlainPlanImpl;
import playground.johannes.plans.view.Person;
import playground.johannes.plans.view.Plan;

/**
 * @author illenberger
 *
 */
public class PersonView extends AbstractView<PlainPersonImpl> implements Person {
	
	private ArrayList<PlanView> plans = new ArrayList<PlanView>();
	
	private List<? extends PlanView> unmodifiablePlans;
	
	public PersonView(PlainPersonImpl plainPerson) {
		super(plainPerson);
		unmodifiablePlans = Collections.unmodifiableList(plans);
	}
	
	public List<? extends PlanView> getPlans() {
		synchronize();
		return unmodifiablePlans;
	}

	@Override
	protected void update() {
		Collection<? extends PlainPlanImpl> newPlans = synchronizeCollections(delegate.getPlans(), plans);
		
		for(PlainPlanImpl p : newPlans) {
			PlanView view = new PlanView(p);
			plans.add(view);
		}
		plans.trimToSize();
	}

	public void addPlan(Plan plan) {
//		if(delegateVersion == delegate.getModCount()) {
//			delegate.addPlan(((PlanView) plan).getDelegate());
//			plans.add((PlanView)plan);
//			delegateVersion = delegate.getModCount();
//		} else {
			delegate.addPlan(((PlanView) plan).getDelegate());
			plans.add((PlanView)plan);
//		}
	}

	public Id getId() {
		return delegate.getId();
	}

	public void removePlan(Plan plan) {
		delegate.removePlan(((PlanView)plan).getDelegate());
		plans.remove(plan);
	}

	public Plan getSelectedPlan() {
		for(PlanView plan : plans) {
			if(plan.getDelegate().equals(delegate.getSelectedPlan())) {
				return plan;
			}
		}
		return null;
	}

	public void setSelectedPlan(Plan plan) {
		delegate.setSelectedPlan(((PlanView)plan).getDelegate());
	}

}
