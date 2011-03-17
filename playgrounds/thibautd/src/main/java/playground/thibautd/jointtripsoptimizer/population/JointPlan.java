/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlan.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.population;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;

/**
 * class for handling synchronized plans.
 * It implements the plan interface to be compatible with the StrategyManager.
 * @author thibautd
 */
public class JointPlan implements Plan {
	private static final Logger log = Logger.getLogger(JointPlan.class);

	private Map<Id,Plan> individualPlans = new HashMap<Id,Plan>();

	//TODO: make final
	private Clique clique;

	private Id currentIndividual = null;
	private Iterator<Id> individualsIterator;

	public JointPlan(Clique clique, Map<Id, ? extends Plan> plans) {
		Plan currentPlan;

		this.clique = clique;
		//TODO: check for consistency (referenced IDs, etc)
		for (Id id: plans.keySet()) {
			currentPlan = new PlanImpl();
			for (PlanElement pe : plans.get(id).getPlanElements()) {
				if (pe instanceof Activity) {
					currentPlan.addActivity(new JointActivity((Activity) pe, 
								this.clique.getMembers().get(id)));
				} else {
					currentPlan.addLeg(new JointLeg((LegImpl) pe,
								(Person) this.clique.getMembers().get(id)));
				}
			}
			this.individualPlans.put(id, currentPlan);
			// add the plan at the individual level
			this.clique.getMembers().get(id).addPlan(currentPlan);
		}
	}

	public JointPlan(JointPlan plan) {
		this(plan.getClique(), plan.getIndividualPlans());
	}

	/*
	 * =========================================================================
	 * Plan interface methods
	 * =========================================================================
	 */
	/**
	 * @return the list of plan elements, for all individuals. While the plan 
	 * elements are internal references, the list is not: modifying it (by adding
	 * or removing elements) will not modify the joint plan.
	 */
	@Override
	public List<PlanElement> getPlanElements() {
		List<PlanElement> output = new ArrayList<PlanElement>();
		for (Plan plan : this.individualPlans.values()) {
			output.addAll(plan.getPlanElements());
		}
		return output;
	}

	/**
	 * Adds a (joint) leg to the plan of the current individual.
	 * Members of the clique must be set before runing this method.
	 * @param leg a JointLeg object, with the participants belonging to the
	 * clique
	 */
	@Override
	public void addLeg(Leg leg) {
		log.warn("using addLeg() on JointPlan: make sure current individual"+
				" is used correctly!");
		if (leg instanceof JointLeg) {
			this.getIndividualPlan(this.getCurrentIndividual()).addLeg(
					(JointLeg) leg);
		} else {
			throw new IllegalArgumentException("trying to add a non-joint"+
					"leg to joint plan: failed.");
		}
	}

	/**
	 * Adds a (joint) activity to the plan.
	 * Members of the clique must be set before runing this method.
	 * @param act a JointActivity object, with the participants belonging to the
	 * clique.
	 */
	@Override
	public void addActivity(Activity act) {
		log.warn("using addActivity() on JointPlan: make sure current individual"+
				" is used correctly!");
		if (act instanceof JointActivity) {
			this.getIndividualPlan(this.getCurrentIndividual()).addActivity(
					(JointActivity) act);
		} else {
			throw new IllegalArgumentException("trying to add a non-joint"+
					"activity to joint plan: failed.");
		}
	}

	@Override
	public boolean isSelected() {
		return this.getPerson().getSelectedPlan() == this;
	}

	@Override
	public void setScore(Double score) {
		throw new UnsupportedOperationException("JointPlan.setScore(Double) is"+
				" unsupported. The scores must be set on the individual plans.");
	}

	/**
	 * Returns the global score as the sum of the individual scores.
	 * @todo in the future, this should include the usage of weights or
	 * the call to an external function.
	 */
	@Override
	public Double getScore() {
		//TODO: call to an external aggregation function, to initialize in the
		//constructor.
		Double score = 0.0;
		for (Plan plan : this.getIndividualPlans().values()) {
			try {
				score += plan.getScore();
			} catch (NullPointerException e) {
				// if at least one of the individual is null, return null
				// (ie unscored).
				return null;
			}
		}
		return score;
	}

	/**
	 * @return the Clique to wich the plan is affected (wrapper to getClique).
	 */
	@Override
	public Person getPerson() {
		log.warn("using getPerson to get clique from JointPlan instance.");
		return this.getClique();
	}

	/**
	 * @param person a Clique to be passed to setClique.
	 */
	@Override
	public void setPerson(Person person) {
		log.warn("using setPerson to set clique from JointPlan instance.");
		try{
			this.setClique((Clique) person);
		} catch (java.lang.ClassCastException e) {
			throw new IllegalArgumentException("unable to set "+person+" in JointPlan: is not a clique!");
		}
	}

	@Override
	public Map<String,Object> getCustomAttributes() {
		//TODO
		return null;
	}

	/*
	 * =========================================================================
	 * JointPlan specific methods
	 * =========================================================================
	 */
	public Clique getClique() {
		return this.clique;
	}

	/**
	 * Sets the clique associated to the joint plan and sets the current
	 * individual to the first member of the clique.
	 * May be moved to a constructor in the future (final clique field).
	 */
	public void setClique(Clique clique) {
		this.clique = clique;
		this.resetCurrentIndividual();
	}

	public Plan getIndividualPlan(Person person) {
		return this.getIndividualPlan(person.getId());
	}

	public Plan getIndividualPlan(Id id) {
		return this.individualPlans.get(id);
	}

	public Map<Id,Plan> getIndividualPlans() {
		return this.individualPlans;
	}

	// public void setCurrentIndividual(Id id) {
	// 	if (this.individualPlans.containsKey(id)) {
	// 		this.currentIndividual = id;
	// 	} else {
	// 		throw new IllegalArgumentException("Trying to set current individual"+
	// 				"to a non-existing individual in JointPlan.");
	// 	}
	// }

	public void resetCurrentIndividual() {
		this.individualsIterator = this.clique.getMembers().keySet().iterator();
		this.currentIndividual = this.individualsIterator.next();
	}

	/**
	 * Jumps to the next individual.
	 * If current individual is the last individual, returns false.
	 */
	public boolean nextIndividual() {
		//TODO
		if (this.individualsIterator.hasNext()) {
			this.currentIndividual = this.individualsIterator.next();
			return true;
		}
		return false;
	}

	public Id getCurrentIndividual() {
		return this.currentIndividual;
	}

	/**
	 * Transforms this plan so that it is identical to the argument plan.
	 * Used in the replanning module.
	 * Caution: this does NOT make a copy of the plan, but makes the internal
	 * individual plan references to be equal. This is OK in the case of the
	 * relanning module (where the argument plan is just a local instance),
	 * but could lead to strange results if the two plan are used in different
	 * places.
	 */
	public void resetFromPlan(JointPlan plan) {
		if (plan.getClique() != this.clique) {
			throw new UnsupportedOperationException("resetting a plan from a plan"
					+" is unsupported.");
		}
		this.individualPlans = plan.getIndividualPlans();
	}

	/**
	 * Sets the individual scores to null.
	 * Used in the replanning.
	 */
	public void resetScores() {
		for (Plan plan : this.individualPlans.values()) {
			plan.setScore(null);
		}
	}

	public List<Activity> getLastActivities() {
		List<Activity> output = new ArrayList<Activity>();
		List<PlanElement> currentPlanElements;

		for (Plan currentPlan : this.individualPlans.values()) {
			currentPlanElements = currentPlan.getPlanElements();
			try {
				output.add((Activity) currentPlanElements.get(currentPlanElements.size() - 1));
			} catch (ClassCastException e) {
				throw new RuntimeException("plan "+currentPlan+" does not finish by an activity.");
			}
		}

		return output;
	}
}

