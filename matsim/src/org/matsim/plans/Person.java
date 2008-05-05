/* *********************************************************************** *
 * project: org.matsim.*
 * Person.java
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

package org.matsim.plans;

import java.util.HashMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicPersonImpl;
import org.matsim.basic.v01.BasicPlan;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;

public class Person extends BasicPersonImpl<Plan>{

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private String type = null;

	private final TreeSet<String> travelcards = new TreeSet<String>();
	private Knowledge knowledge = null;
	private String visualizerData = null;
	
	private final static Logger log = Logger.getLogger(Person.class);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////


	public Person(final Id id, final String sex, final int age, final String license,
			final String carAvail, final String employed) {
		super(id);
		int intID = Integer.parseInt(id.toString());
		if (intID < 0) {
			throw new NumberFormatException("A person's id has to be an integer >= 0.");
		}
		if (sex != null) { // (m,f,null)
			this.setSex(sex.intern());
		}
		this.setAge(age);
		if ((this.getAge() < 0) && (this.getAge() != Integer.MIN_VALUE)) {
				throw new NumberFormatException("A person's age has to be an integer >= 0.");
			}
		if (license != null) {
			this.setLicence(license.intern());
		}
		if (carAvail != null) {
			this.setCarAvail(carAvail.intern());
		}
		if (employed != null) {
			this.setEmployed(employed.intern());
		}
	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final Knowledge createKnowledge(final String desc) {
		if (this.knowledge == null) {
			this.knowledge = new Knowledge(desc);
		}
		return this.knowledge;
	}

	public final Plan createPlan(final String score, final String selected) {
		Plan p = new Plan(score, this);
		this.plans.add(p);
		if (selected.equals("yes")) {	setSelectedPlan(p); }
		else if (!selected.equals("no")) {
			throw new NumberFormatException("Attribute 'selected' of Element 'Plan' is neither 'yes' nor 'no'.");
		}
		// Make sure there is a selected plan if there is at least one plan
		if (this.selectedPlan == null) this.selectedPlan = p;
		return p;
	}

	//////////////////////////////////////////////////////////////////////
	// add methods
	//////////////////////////////////////////////////////////////////////

	public final void addTravelcard(final String type) {
		if (this.travelcards.contains(type)) {
			log.info(this + "[type=" + type + " already exists]");
		} else {
			this.travelcards.add(type.intern());
		}
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public final void setType(final String type) {
		this.type = (type == null) ? null : type.intern();
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final TreeSet<String> getTravelcards() {
		return this.travelcards;
	}

	public final Knowledge getKnowledge() {
		return this.knowledge;
	}

	public final String getType() {
		return this.type;
	}

	/**
	 * Returns a random plan from the list of all plans this agent has.
	 *
	 * @return A random plan, or <code>null</code> if none such plan exists
	 */
	public Plan getRandomPlan() {
		if (this.plans.size() == 0) {
			return null;
		}
		int index = (int)(Gbl.random.nextDouble()*this.plans.size());
		return this.plans.get(index);
	}

	/**
	 * Returns a plan with undefined score, chosen randomly among all plans
	 * with undefined score.
	 *
	 * @return A random plan with undefined score, or <code>null</code> if none such plan exists.
	 */
	public Plan getRandomUnscoredPlan() {
		int cntUnscored = 0;
		for (Plan plan : this.plans) {
			if (plan.hasUndefinedScore()) {
				cntUnscored++;
			}
		}
		if (cntUnscored > 0) {
			// select one of the unscored plans
			int idxUnscored = Gbl.random.nextInt(cntUnscored);
			cntUnscored = 0;
			for (Plan plan : this.plans) {
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
			this.plans.add(newPlan);
		} else {
			int i = this.plans.indexOf(oldSelectedPlan);
			this.plans.set(i, newPlan);
		}
		setSelectedPlan(newPlan);
	}

	/**
	 * Makes a copy of the currently selected plan, marking the copy as selected.
	 *
	 * @return the copy of the selected plan. Returns null if there is no selected plan.
	 */
	public Plan copySelectedPlan() {
		int i=0;
		Plan oldPlan = this.getSelectedPlan();
		if (oldPlan == null) {
			return null;
		}
		Plan newPlan = new Plan(oldPlan.getPerson());
		try {
			newPlan.copyPlan(oldPlan);
			this.plans.add(newPlan);
			this.setSelectedPlan(newPlan);
		} catch (Exception e) {
			log.warn("plan# " + i +" went wrong:", e);
			newPlan = oldPlan; // give old plan back??
		}
		return newPlan;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[id=" + this.getId() + "]" +
				"[sex=" + this.getSex() + "]" +
				"[age=" + this.getAge() + "]" +
				"[license=" + this.getLicense() + "]" +
				"[car_avail=" + this.getCarAvail() + "]" +
				"[employed=" + this.getEmployed() + "]" +
				"[nof_travelcards=" + this.travelcards.size() + "]" +
				"[knowledge=" + this.knowledge + "]" +
				"[nof_plans=" + this.plans.size() + "]";
	}

	/** Removes the plans with the worst score until only <code>maxSize</code> plans are left.
	 * Plans with undefined scores are treated as plans with very bad scores and thus removed
	 * first. If there are several plans with the same bad score, it can not be predicted which
	 * one of them will be removed.<br>
	 * This method insures that if there are different types of plans (see
	 * {@link org.matsim.plans.Plan#getType()}),
	 * at least one plan of each type remains. This could lead to the worst plan being kept if
	 * it is the only one of it's type. Plans with type <code>null</code> are handled like any
	 * other type, and are differentiated from plans with the type set to an empty String.<br>
	 *
	 * If there are more plan-types than <code>maxSize</code>, it is not possible to reduce the
	 * number of plans to the requested size.<br>
	 *
	 * If the selected plan is on of the deleted ones, a randomly chosen plan will be selected.
	 *
	 * @param maxSize The number of plans that should be left.
	 */
	public void removeWorstPlans(final int maxSize) {
		if (this.plans.size() <= maxSize) {
			return;
		}
		HashMap<Plan.Type, Integer> typeCounts = new HashMap<Plan.Type, Integer>();
		// initialize list of types
		for (Plan plan : this.plans) {
			Integer cnt = typeCounts.get(plan.getType());
			if (cnt == null) {
				typeCounts.put(plan.getType(), Integer.valueOf(1));
			} else {
				typeCounts.put(plan.getType(), Integer.valueOf(cnt.intValue() + 1));
			}
		}
		while (this.plans.size() > maxSize) {
			Plan worst = null;
			double worstScore = Double.POSITIVE_INFINITY;
			for (Plan plan : this.plans) {
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
				this.plans.remove(worst);
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

	/**
	 * @param visualizerData sets the optional user data for visualizer
	 */
	public void setVisualizerData(final String visualizerData) {
		this.visualizerData = visualizerData;
	}

	/**
	 *
	 * @return Returns the visualizer data
	 */
	public String getVisualizerData() {
		return this.visualizerData ;
	}

}
