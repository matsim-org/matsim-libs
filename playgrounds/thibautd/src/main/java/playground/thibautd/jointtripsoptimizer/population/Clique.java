/* *********************************************************************** *
 * project: org.matsim.*
 * Clique.java
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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import org.matsim.core.gbl.MatsimRandom;

import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

/**
 * TODO: implement
 * @author thibautd
 */
public class Clique implements Person {
	private static final Logger log =
		Logger.getLogger(Clique.class);

	// private fields
	private Id id;
	private HashMap<Id,Person> members;
	private ArrayList<JointPlan> plans;
	private JointPlan selectedPlan;

	/*
	 * =========================================================================
	 * constructors
	 * =========================================================================
	 */
	public Clique(Id id) {
		this.members = new HashMap<Id,Person>();
		this.plans = new ArrayList<JointPlan>();
		this.selectedPlan = null;
		this.setId(id);
	}

	/*
	 * =========================================================================
	 * Person interface methods
	 * =========================================================================
	 */
	/**
	 * @return the<i>joint</i> plans of the clique.
	 */
	@Override
	public List<? extends Plan> getPlans() {
		//log.debug("clique.getPlans() returns "+this.plans.size()+" plans");
		//log.debug("first individual has "+
		//		((Person) members.values().toArray()[0]).getPlans().size()+" plans");
		return this.plans;
	}

	@Override
	public void setId(Id id) {
		this.id = id;
	}

	@Override
	public boolean addPlan(Plan p) {
		//caster à joint plan
		//verifier que les participants sont bien membres de la clique
		log.warn("using yet unimplemented clique.addPlan method:");
		return false;
	}

	@Override
	public Plan getSelectedPlan() {
		return this.selectedPlan;
	}

	@Override
	public Id getId() {
		return this.id;
	}

	@Override
	public Map<String,Object> getCustomAttributes() {
		return null;
	}

	/*
	 * ========================================================================
	 * methods existing for PersonImpl but not in the interface
	 * ========================================================================
	 */
	public final void setSelectedPlan(final JointPlan selectedPlan) {
		if (this.getPlans().contains(selectedPlan)) {
			this.selectedPlan = selectedPlan;
			// select the plan at the individual level too:
			for(Person individual : this.getMembers().values()) {
				((PersonImpl) individual).setSelectedPlan(
					(PlanImpl) selectedPlan.getIndividualPlan(individual));
			}
		} else if (selectedPlan != null) {
			throw new IllegalStateException("The joint plan to be set as selected is not stored in the clique's plans");
		}
	}

	public final void setSelectedPlan(final Plan selectedPlan) {
		this.setSelectedPlan((JointPlan) selectedPlan);
	}

	public Plan getRandomUnscoredPlan() {
		// Code taken from PersonImpl
		int cntUnscored = 0;
		for (Plan plan : this.getPlans()) {
			if (plan.getScore() == null) {
				cntUnscored++;
			}
		}
		if (cntUnscored > 0) {
			// select one of the unscored plans
			int idxUnscored = MatsimRandom.getRandom().nextInt(cntUnscored);
			cntUnscored = 0;
			for (Plan plan : this.getPlans()) {
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

	public Plan getRandomPlan() {
		//code taken from PersonImpl
		if (this.getPlans().size() == 0) {
			return null;
		}
		int index = (int)(MatsimRandom.getRandom().nextDouble()*this.getPlans().size());
		return this.getPlans().get(index);
	}

	public Plan copySelectedPlan() {
		JointPlan plan = new JointPlan(this.selectedPlan);
		//TODO: use this.addPlan (when implemented)
		this.plans.add(plan);
		this.selectedPlan = plan;
		return plan;
	}

	/*
	 * =========================================================================
	 * Clique specific methods
	 * =========================================================================
	 */
	public Map<Id, ? extends Person> getMembers() {
		return this.members;
	}

	public void addMember(Person p) {
		this.members.put(p.getId(),p);
	}

	/**
	 * Not pertinent for households, but here for completeness.
	 */
	public void removeMember(Person p) {
		//TODO: check if person belongs to clique and log error/throw an exception if not
		this.members.remove(p.getId());
	}

	public void removePlan(Plan plan) {
		if ((plan instanceof JointPlan)&&(this.plans.remove(plan))) {
			// delete the corresponding individual plans
			for (Person person : this.getMembers().values()) {
				person.getPlans().remove(
						((JointPlan) plan).getIndividualPlan(person) );
			}
		} else {
			throw new IllegalArgumentException("trying to remove non existing "+
					"plan from clique");
		}
	}

	/**
	 * Builds a joint plan based on the (presumed unique) plans of the members.
	 * To call immediately after having added all members.
	 * TODO: buil it from extra information from the plans file.
	 * TODO: initialize at construction? (possibility of choosing between dataset
	 * extraction, joint trips insertion heuristics, choice model...)
	 * 
	 */
	public void buildJointPlanFromIndividualPlans() {
		//TODO: create JointTrips
		if (this.plans.isEmpty()) {
			Map<Id, PlanImpl> individualPlans = new HashMap<Id, PlanImpl>();
			JointPlan newJointPlan;
			
			for (Person member : this.getMembers().values()) {
				if (member.getPlans().size()>1) {
					log.warn("only keeping the selected plan for agent "+member+" with multiple plans");
					individualPlans.put(member.getId(), (PlanImpl) member.getSelectedPlan());
				} else {
					individualPlans.put(member.getId(), (PlanImpl) member.getPlans().get(0));
				}
			}
			
			this.clearIndividualPlans();
			newJointPlan = new JointPlan(this, individualPlans);
			//TODO: use this.addPlan (when implemented)
			this.plans.add(newJointPlan);
			this.setSelectedPlan(newJointPlan);
		} else {
			throw new UnsupportedOperationException(
					"Clique.buildJointPlanFromIndividualPlans() cannot be ran "+
					"when the clique already contains joint plans.");
		}
	}

	private void clearIndividualPlans() {
		for (Person member : this.getMembers().values()) {
			member.getPlans().clear();
		}
	}
}

