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
 * Defines a "clique" agregating several agents.
 *
 * The "clique", in the context of the joint plan optimisation,
 * represents a group of agents which plans are interdependant.
 *
 * It constitutes the level at which strategies are processed.
 *
 * @author thibautd
 */
public class Clique implements Person {
	private static final Logger log =
		Logger.getLogger(Clique.class);

	// import/export package visible fields
	static final String PLAN_TYPE_SEP = "_";
	static final String PLAN_TYPE_PREFIX = "linkedPlan";
	static final String PLAN_TYPE_REGEXP = PLAN_TYPE_PREFIX + PLAN_TYPE_SEP + ".*";

	private int planCount = 0;

	// private fields
	private final Id id;
	private final Map<Id,Person> members;
	private final List<JointPlan> plans;
	private JointPlan selectedPlan;

	/*
	 * =========================================================================
	 * constructors
	 * =========================================================================
	 */
	/**
	 * @deprecated
	 */
	public Clique(final Id id) {
		this(id, new HashMap<Id, Person>());
	}

	public Clique(final Id id, final Map<Id, Person> members) {
		this.members = members;
		this.plans = new ArrayList<JointPlan>();
		this.selectedPlan = null;
		this.id = id;
	}

	/*
	 * =========================================================================
	 * Person interface methods
	 * =========================================================================
	 */
	/**
	 * @return the <i>joint</i> plans of the clique.
	 */
	@Override
	public List<? extends Plan> getPlans() {
		//log.debug("clique.getPlans() returns "+this.plans.size()+" plans");
		//log.debug("first individual has "+
		//		((Person) members.values().toArray()[0]).getPlans().size()+" plans");
		return this.plans;
	}

	/**
	 * Unimplemented method: changing id after initialisation may lead to inconsistencies.
	 *
	 * @deprecated
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void setId(Id id) {
		//this.id = id;
		throw new UnsupportedOperationException("cannot change clique id");
	}

	/**
	 * Currently unimplemented.
	 * @deprecated
	 * @throws UnsupportedOperationException
	 */
	@Override
	public boolean addPlan(Plan p) {
		//cast to joint plan
		//check clique consistency
		throw new UnsupportedOperationException("using yet unimplemented clique.addPlan method:");
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
		this.setSelectedPlan(plan);
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

	/**
	 * This should be moved in the constructor some day.
	 * Currently used to initialize clique.
	 * @deprecated
	 */
	public void addMember(final Person p) {
		this.members.put(p.getId(),p);
	}

	/**
	 * Unimplemented, as it would break the consistency between clique and JointPlan.
	 *
	 * May, or not, be implemented in the future to allow dynamic cliques.
	 *
	 * @deprecated
	 * @throws UnsupportedOperationException
	 */
	public void removeMember(final Person p) {
		throw new UnsupportedOperationException("currently impossible to remove"+
				" a member from a clique");
	}

	public boolean removePlan(final Plan plan) {
		if ((plan instanceof JointPlan)&&(this.plans.remove(plan))) {
			// delete the corresponding individual plans
			for (Person person : this.getMembers().values()) {
				//((PersonImpl) person).removePlan(
				if (!person.getPlans().remove(
						((JointPlan) plan).getIndividualPlan(person) )) {
					throw new RuntimeException("plan removal failed at the individual level!");
				}
			}
			if (this.getSelectedPlan() == plan) {
				this.setSelectedPlan(this.getRandomPlan());
			}
			return true;
		} else {
			log.warn("Clique.removePlan(Plan) has been launched on an invalid plan");
			return false;
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// plan import/export
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Builds a joint plan based on the (presumed unique) plans of the members.
	 * To call immediately after having added all members.
	 * TODO: buil it from extra information from the plans file.
	 * TODO: initialize at construction? (possibility of choosing between dataset
	 * extraction, joint trips insertion heuristics, choice model...)
	 */
	public void buildJointPlanFromIndividualPlans() {
		if (this.plans.isEmpty()) {
			String defaultType = "a";
			String selected = defaultType;
			Map<String, Map<Id, PlanImpl>> individualPlans = new HashMap<String, Map<Id, PlanImpl>>();
			JointPlan newJointPlan;
			PlanImpl currentPlan;
			List<? extends Plan> memberPlans;
			boolean toSynchronize = true;
			
			for (Person member : this.getMembers().values()) {
				memberPlans = member.getPlans();

				if (memberPlans.size()>1) {
					if ( (((PlanImpl) memberPlans.get(0)).getType() == null) ||
							(!((PlanImpl) memberPlans.get(0)).getType().matches(PLAN_TYPE_REGEXP)) ) {
						log.warn("only keeping the selected plan for agent "+
								member.getId()+" with multiple plans");
						currentPlan = (PlanImpl) member.getSelectedPlan();
						getPlanMap(defaultType, individualPlans)
							.put(member.getId(), currentPlan);
					}
					else {
						// assume that if a plan as a proper type, the others do
						for (Plan currentMemberPlan : memberPlans) {
							currentPlan = (PlanImpl) currentMemberPlan;
							getPlanMap(currentPlan.getType(), individualPlans)
								.put(member.getId(), currentPlan);
							if (currentPlan.isSelected()) {
								selected = currentPlan.getType();
							}
						}
						// assume that if several plans are given, they are synchronized
						// (TODO pass by the config)
						toSynchronize = false;
					}
				}
				else {
					currentPlan = (PlanImpl) memberPlans.get(0);
					getPlanMap(defaultType, individualPlans)
						.put(member.getId(), currentPlan);
				}
			}
			
			this.clearIndividualPlans();
			this.plans.clear();

			for (Map.Entry<String, Map<Id, PlanImpl>> entry : individualPlans.entrySet()) {
				newJointPlan = new JointPlan(
						this,
						entry.getValue(),
						true, //add at individual level
						toSynchronize);
				//TODO: use this.addPlan (when implemented)
				this.plans.add(newJointPlan);
				if (entry.getKey().equals(selected)) {
					this.setSelectedPlan(newJointPlan);
				}
			}
		}
		else {
			throw new UnsupportedOperationException(
					"Clique.buildJointPlanFromIndividualPlans() cannot be ran "+
					"when the clique already contains joint plans.");
		}
	}

	private Map<Id, PlanImpl> getPlanMap(
			final String type,
			final Map<String, Map<Id, PlanImpl>> individualPlans) {
		Map<Id, PlanImpl> out = individualPlans.get(type);
		
		if (out == null) {
			out = new HashMap<Id, PlanImpl>();
			individualPlans.put(type, out);
		}

		return out;
	}

	private void clearIndividualPlans() {
		for (Person member : this.getMembers().values()) {
			member.getPlans().clear();
		}
	}

	/**
	 * @return a type, meant to identify individual plans pertaining to the same
	 * joint plan at import.
	 */
	/*package*/ String getNextIndividualPlanType() {
		planCount++;
		return PLAN_TYPE_PREFIX + PLAN_TYPE_SEP + planCount;
	}
}

