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
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.experimental.population.Person;
import org.matsim.core.basic.v01.BasicPersonImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.households.Household;
import org.matsim.knowledges.Knowledge;
import org.matsim.population.Desires;
import org.matsim.utils.customize.Customizable;
import org.matsim.utils.customize.CustomizableImpl;
/**
 * Default implementation of {@link PersonImpl} interface.
 * 
 * @see org.matsim.core.population.PersonImpl
 */
public class PersonImpl implements Person {

	private final static Logger log = Logger.getLogger(PersonImpl.class);

	private final BasicPersonImpl<PlanImpl> delegate;

	private Customizable customizableDelegate;

	private Household household;


	public PersonImpl(final Id id) {
		this.delegate = new BasicPersonImpl<PlanImpl>(id);
	}

//	public void addPlan(final PlanImpl plan) {
//		this.delegate.addPlan(plan);
//		// Make sure there is a selected plan if there is at least one plan
//		if (this.selectedPlan == null) this.selectedPlan = plan;
//	}

	public final PlanImpl getSelectedPlan() {
		return this.delegate.getSelectedPlan();
	}
//
	public final void setSelectedPlan(final PlanImpl selectedPlan) {
		this.delegate.setSelectedPlan(selectedPlan);
	}
	
	public final boolean addPlan(PlanImpl p){
		return this.delegate.addPlan(p);
	}

	public PlanImpl createPlan(final boolean selected) {
		PlanImpl p = new PlanImpl(this);
		this.delegate.getPlans().add(p);
		if (selected) {
			this.delegate.setSelectedPlan(p);
		}
		// Make sure there is a selected plan if there is at least one plan
		if (this.getSelectedPlan() == null)
			this.delegate.setSelectedPlan(p);
		return p;
	}

	public void removeUnselectedPlans() {
		for (Iterator<PlanImpl> iter = this.delegate.getPlans().iterator(); iter.hasNext(); ) {
			PlanImpl plan = iter.next();
			if (!plan.isSelected()) {
				iter.remove();
			}
		}
	}

	public PlanImpl getRandomPlan() {
		if (this.delegate.getPlans().size() == 0) {
			return null;
		}
		int index = (int)(MatsimRandom.getRandom().nextDouble()*this.delegate.getPlans().size());
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
			this.delegate.getPlans().add(newPlan);
		} else {
			int i = this.delegate.getPlans().indexOf(oldSelectedPlan);
			this.delegate.getPlans().set(i, newPlan);
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
		this.delegate.getPlans().add(newPlan);
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
		b.append("[knowledge=").append(this.getKnowledge() == null ? "null" : this.getKnowledge()).append("]");
		b.append("[nof_plans=").append(this.getPlans() == null ? "null" : this.getPlans().size()).append("]");
	  return b.toString();
	}

	public boolean removePlan(final PlanImpl plan) {
		boolean result = this.delegate.getPlans().remove(plan);
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

	public Knowledge createKnowledge(final String desc) {
		if (this.delegate.getKnowledge() == null) {
			Knowledge k = new Knowledge();
			k.setDescription(desc);
			this.delegate.setKnowledge(k);
		}
		return (Knowledge) this.delegate.getKnowledge();
	}

	public void addTravelcard(final String type) {
		this.delegate.addTravelcard(type);
	}

	public Desires createDesires(final String desc) {
		return this.delegate.createDesires(desc);
	}

	public int getAge() {
		return this.delegate.getAge();
	}

	public String getCarAvail() {
		return this.delegate.getCarAvail();
	}

	public Desires getDesires() {
		return this.delegate.getDesires();
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

	public Id getId() {
		return this.delegate.getId();
	}

	public Knowledge getKnowledge() {
		return (Knowledge) this.delegate.getKnowledge();
	}

	public String getLicense() {
		return this.delegate.getLicense();
	}

	public List<PlanImpl> getPlans() {
		return this.delegate.getPlans();
	}

	public String getSex() {
		return this.delegate.getSex();
	}

	public TreeSet<String> getTravelcards() {
		return this.delegate.getTravelcards();
	}

	public boolean hasLicense() {
		return this.delegate.hasLicense();
	}

	public Boolean isEmployed() {
		return this.delegate.isEmployed();
	}

	public void setAge(final int age) {
		this.delegate.setAge(age);
	}

	public void setCarAvail(final String carAvail) {
		this.delegate.setCarAvail(carAvail);
	}

	public void setEmployed(final String employed) {
		this.delegate.setEmployed(employed);
	}

	public void setId(final Id id) {
		this.delegate.setId(id);
	}

	public void setLicence(final String licence) {
		this.delegate.setLicence(licence);
	}

	public void setSex(final String sex) {
		this.delegate.setSex(sex);
	}

	public Map<String, Object> getCustomAttributes() {
		if (this.customizableDelegate == null) {
			this.customizableDelegate = new CustomizableImpl();
		}
		return this.customizableDelegate.getCustomAttributes();
	}

	public void setKnowledge(final Knowledge knowledge) {
		this.delegate.setKnowledge(knowledge);
	}

}
