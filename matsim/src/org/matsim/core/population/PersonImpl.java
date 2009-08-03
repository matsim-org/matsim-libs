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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.population.BasicPersonImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.households.Household;
import org.matsim.utils.customize.Customizable;
import org.matsim.utils.customize.CustomizableImpl;
/**
 * Default implementation of {@link PersonImpl} interface.
 * 
 * @see org.matsim.core.population.PersonImpl
 */
public class PersonImpl extends BasicPersonImpl<Plan> implements Person {

	private final static Logger log = Logger.getLogger(PersonImpl.class);

	private Customizable customizableDelegate;

	private Household household;


	public PersonImpl(final Id id) {
		super(id);
	}

	@Override
	public final PlanImpl getSelectedPlan() {
		return (PlanImpl) super.getSelectedPlan();
	}


	public PlanImpl createPlan(final boolean selected) {
		PlanImpl p = new PlanImpl(this);
		this.addPlan(p);
		if (selected) {
			this.setSelectedPlan(p);
		}
		return p;
	}

	public void removeUnselectedPlans() {
		for (Iterator<PlanImpl> iter = this.getPlans().iterator(); iter.hasNext(); ) {
			PlanImpl plan = iter.next();
			if (!plan.isSelected()) {
				iter.remove();
			}
		}
	}

	public PlanImpl getRandomPlan() {
		if (this.getPlans().size() == 0) {
			return null;
		}
		int index = (int)(MatsimRandom.getRandom().nextDouble()*this.getPlans().size());
		return this.getPlans().get(index);
	}

	public PlanImpl getRandomUnscoredPlan() {
		int cntUnscored = 0;
		for (PlanImpl plan : this.getPlans()) {
			if (plan.getScore() == null) {
				cntUnscored++;
			}
		}
		if (cntUnscored > 0) {
			// select one of the unscored plans
			int idxUnscored = MatsimRandom.getRandom().nextInt(cntUnscored);
			cntUnscored = 0;
			for (PlanImpl plan : this.getPlans()) {
				if (plan.getScore() == null) {
					if (cntUnscored == idxUnscored) {
						return plan;
					}
					cntUnscored++;
				}
			}
		}
		return null;
	}

	public void exchangeSelectedPlan(final PlanImpl newPlan, final boolean appendPlan) {
		newPlan.setPerson(this);
		PlanImpl oldSelectedPlan = getSelectedPlan();
		if (appendPlan || (oldSelectedPlan == null)) {
			this.getPlans().add(newPlan);
		} else {
			int i = this.getPlans().indexOf(oldSelectedPlan);
			this.getPlans().set(i, newPlan);
		}
		setSelectedPlan(newPlan);
	}

	public PlanImpl copySelectedPlan() {
		PlanImpl oldPlan = this.getSelectedPlan();
		if (oldPlan == null) {
			return null;
		}
		PlanImpl newPlan = new PlanImpl(oldPlan.getPerson());
		newPlan.copyPlan(oldPlan);
		this.getPlans().add(newPlan);
		this.setSelectedPlan(newPlan);
		return newPlan;
	}


	@Override
	public final String toString() {
		StringBuilder b = new StringBuilder();
		b.append("[id=").append(this.getId()).append("]");
		b.append("[sex=").append(this.getSex()).append("]");
		b.append("[age=").append(this.getAge()).append("]");
		b.append("[license=").append(this.getLicense()).append("]");
		b.append("[car_avail=").append(this.getCarAvail()).append("]");
		b.append("[employed=").append(this.getEmployed()).append("]");
		b.append("[travelcards=").append(this.getTravelcards() == null ? "null" : this.getTravelcards().size()).append("]");
		b.append("[nof_plans=").append(this.getPlans() == null ? "null" : this.getPlans().size()).append("]");
	  return b.toString();
	}

	public boolean removePlan(final PlanImpl plan) {
		boolean result = this.getPlans().remove(plan);
		if ((this.getSelectedPlan() == plan) && result) {
			this.setSelectedPlan(this.getRandomPlan());
		}
		return result;
	}

	public Id getHouseholdId() {
		if (this.household != null) {
			return this.household.getId();
		}
		return null;
	}

	public Household getHousehold() {
		return this.household;
	}

	public void setHousehold(final Household hh) {
		if (!hh.getMembers().containsKey(this.getId())) {
			hh.getMembers().put(this.getId(), this);
			this.household = hh;
		}
		else if (this.household == null) {
			this.household = hh;
		}
		else if (!this.equals(hh.getMembers().get(this.getId()))) {
			throw new IllegalStateException("The household with id: " + hh.getId() + " already has a member"
					+ " with id: " + this.getId() + " the referenced objects however are not equal!");
		}
	}

	
	/**
	 * @return "yes" if the person has a job
	 * @deprecated use {@link #isEmployed()}
	 */
	@Deprecated
	public String getEmployed() {
		if (isEmployed() == null) {
			return null;
		}
		return (isEmployed() ? "yes" : "no");
	}

	@Override
	public List<PlanImpl> getPlans() {
		return (List<PlanImpl>) super.getPlans();
	}


	public Map<String, Object> getCustomAttributes() {
		if (this.customizableDelegate == null) {
			this.customizableDelegate = new CustomizableImpl();
		}
		return this.customizableDelegate.getCustomAttributes();
	}

}
