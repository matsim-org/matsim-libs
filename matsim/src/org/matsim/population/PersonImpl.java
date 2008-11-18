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

package org.matsim.population;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicPerson;
import org.matsim.basic.v01.BasicPersonImpl;
import org.matsim.basic.v01.BasicPlan;
import org.matsim.basic.v01.Id;
import org.matsim.gbl.MatsimRandom;
import org.matsim.utils.customize.Customizable;
import org.matsim.utils.customize.CustomizableImpl;
/**
 * For comments see interface
 * @see org.matsim.population.Person
 * @author dgrether
 *
 */
public class PersonImpl implements Person{

	private final static Logger log = Logger.getLogger(Person.class);

	private BasicPerson<Plan, Knowledge> delegate;
	
	private Customizable customizableDelegate;
	
	private String visualizerData = null;

	private Household household;

	
	public PersonImpl(Id id) {
		this.delegate = new BasicPersonImpl(id);
//		super(id);
	}
	
	public Plan createPlan(final boolean selected) {
		Plan p = new Plan(this);
		this.delegate.getPlans().add(p);
		if (selected) {
			setSelectedPlan(p);
		}
		// Make sure there is a selected plan if there is at least one plan
		if (this.delegate.getSelectedPlan() == null) 
			this.delegate.setSelectedPlan(p);
		return p;
	}
	
	public void removeUnselectedPlans() {
		for (Iterator<Plan> iter = this.delegate.getPlans().iterator(); iter.hasNext(); ) {
			Plan plan = iter.next();
			if (!plan.isSelected()) {
				iter.remove();
			}
		}
	}

	public Plan getRandomPlan() {
		if (this.delegate.getPlans().size() == 0) {
			return null;
		}
		int index = (int)(MatsimRandom.random.nextDouble()*this.delegate.getPlans().size());
		return this.delegate.getPlans().get(index);
	}

	public Plan getRandomUnscoredPlan() {
		int cntUnscored = 0;
		for (Plan plan : this.delegate.getPlans()) {
			if (plan.hasUndefinedScore()) {
				cntUnscored++;
			}
		}
		if (cntUnscored > 0) {
			// select one of the unscored plans
			int idxUnscored = MatsimRandom.random.nextInt(cntUnscored);
			cntUnscored = 0;
			for (Plan plan : this.delegate.getPlans()) {
				if (plan.hasUndefinedScore()) {
					if (cntUnscored == idxUnscored) {
						return plan;
					}
					cntUnscored++;
				}
			}
		}
		return null;
	}

	public void exchangeSelectedPlan(final Plan newPlan, final boolean appendPlan) {
		newPlan.setPerson(this);
		BasicPlan oldSelectedPlan = getSelectedPlan();
		if (appendPlan || (oldSelectedPlan == null)) {
			this.delegate.getPlans().add(newPlan);
		} else {
			int i = this.delegate.getPlans().indexOf(oldSelectedPlan);
			this.delegate.getPlans().set(i, newPlan);
		}
		setSelectedPlan(newPlan);
	}

	public Plan copySelectedPlan() {
		int i=0;
		Plan oldPlan = this.getSelectedPlan();
		if (oldPlan == null) {
			return null;
		}
		Plan newPlan = new Plan(oldPlan.getPerson());
		try {
			newPlan.copyPlan(oldPlan);
			this.delegate.getPlans().add(newPlan);
			this.setSelectedPlan(newPlan);
		} catch (Exception e) {
			log.warn("plan# " + i +" went wrong:", e);
			newPlan = oldPlan; // give old plan back??
		}
		return newPlan;
	}


	@Override
	public final String toString() {
		return "[id=" + this.getId() + "]" +
				"[sex=" + this.getSex() + "]" +
				"[age=" + this.getAge() + "]" +
				"[license=" + this.getLicense() + "]" +
				"[car_avail=" + this.getCarAvail() + "]" +
				"[employed=" + this.getEmployed() + "]" +
				"[nof_travelcards=" + this.getTravelcards().size() + "]" +
				"[knowledge=" + this.getKnowledge() + "]" +
				"[nof_plans=" + this.delegate.getPlans().size() + "]";
	}

	public boolean removePlan(final Plan plan) {
		boolean result = this.delegate.getPlans().remove(plan);
		if ((this.delegate.getSelectedPlan() == plan) && result) {
			this.setSelectedPlan(this.getRandomPlan());
		}
		return result;
	}
	
	public void removeWorstPlans(final int maxSize) {
		if (this.delegate.getPlans().size() <= maxSize) {
			return;
		}
		HashMap<Plan.Type, Integer> typeCounts = new HashMap<Plan.Type, Integer>();
		// initialize list of types
		for (Plan plan : this.delegate.getPlans()) {
			Integer cnt = typeCounts.get(plan.getType());
			if (cnt == null) {
				typeCounts.put(plan.getType(), Integer.valueOf(1));
			} else {
				typeCounts.put(plan.getType(), Integer.valueOf(cnt.intValue() + 1));
			}
		}
		while (this.delegate.getPlans().size() > maxSize) {
			Plan worst = null;
			double worstScore = Double.POSITIVE_INFINITY;
			for (Plan plan : this.delegate.getPlans()) {
				if (typeCounts.get(plan.getType()).intValue() > 1) {
					if (Plan.isUndefinedScore(plan.getScore())) {
						worst = plan;
						// make sure no other score could be less than this
						worstScore = Double.NEGATIVE_INFINITY;
					} else if (plan.getScore() < worstScore) {
						worst = plan;
						worstScore = plan.getScore();
					}
				}
			}
			if (worst != null) {
				this.delegate.getPlans().remove(worst);
				if (worst.isSelected()) {
					this.setSelectedPlan(this.getRandomPlan());
				}
				// reduce the number of plans of this type
				Integer cnt = typeCounts.get(worst.getType());
				typeCounts.put(worst.getType(), Integer.valueOf(cnt.intValue() - 1));
			} else {
				return; // should only happen if we have more different plan-types than maxSize
			}
		}
	}

	public Id getFiscalHouseholdId() {
		if (this.household != null) {
			return this.household.getId();
		}
		return null;
	}
	
	public Household getHousehold() {
		return this.household;
	}

	public void setHousehold(Household hh) {
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
		if (delegate.getKnowledge() == null) {
			Knowledge k = new Knowledge();
			k.setDescription(desc);
			((BasicPersonImpl)delegate).setKnowledge(k);
		}
		return delegate.getKnowledge();
	}
	
	public void setVisualizerData(final String visualizerData) {
		this.visualizerData = visualizerData;
	}

	public String getVisualizerData() {
		return this.visualizerData ;
	}

	public void addPlan(Plan plan) {
		delegate.addPlan(plan);
	}

	public void addTravelcard(String type) {
		delegate.addTravelcard(type);
	}

	public Desires createDesires(String desc) {
		return delegate.createDesires(desc);
	}

	public int getAge() {
		return delegate.getAge();
	}

	public String getCarAvail() {
		return delegate.getCarAvail();
	}

	public Desires getDesires() {
		return delegate.getDesires();
	}

	public String getEmployed() {
		return delegate.getEmployed();
	}

	public Id getId() {
		return delegate.getId();
	}

	public Knowledge getKnowledge() {
		return delegate.getKnowledge();
	}

	public String getLicense() {
		return delegate.getLicense();
	}

	public List<Plan> getPlans() {
		return delegate.getPlans();
	}

	public Plan getSelectedPlan() {
		return delegate.getSelectedPlan();
	}

	public String getSex() {
		return delegate.getSex();
	}

	public TreeSet<String> getTravelcards() {
		return delegate.getTravelcards();
	}

	public boolean hasLicense() {
		return delegate.hasLicense();
	}

	public boolean isEmployed() {
		return delegate.isEmployed();
	}

	public void setAge(int age) {
		delegate.setAge(age);
	}

	public void setCarAvail(String carAvail) {
		delegate.setCarAvail(carAvail);
	}

	public void setEmployed(String employed) {
		delegate.setEmployed(employed);
	}

	public void setId(Id id) {
		delegate.setId(id);
	}

	public void setLicence(String licence) {
		delegate.setLicence(licence);
	}

	public void setSelectedPlan(Plan selectedPlan) {
		delegate.setSelectedPlan(selectedPlan);
	}

	public void setSex(String sex) {
		delegate.setSex(sex);
	}

	public Map<String, Object> getCustomAttributes() {
		if (this.customizableDelegate == null) {
			this.customizableDelegate = new CustomizableImpl();
		}
		return this.customizableDelegate.getCustomAttributes();
	}

	public void setKnowledge(Knowledge knowledge) {
		((BasicPersonImpl)this.delegate).setKnowledge(knowledge);
	}



}
