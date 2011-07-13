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

	private static boolean warnKeepOnlySelected = true;

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
	 * @deprecated should be removed soon.
	 */
	public Clique(final Id id) {
		this(id, new HashMap<Id, Person>());
	}

	/**
	 * constructs a clique.
	 *
	 * @param id the {@link Id} of the clique
	 * @param members a map listing members and their {@link Id}s
	 */
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
	public List<JointPlan> getPlans() {
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
	 * Currently unimplemented. The only way to create a new plan is currently by
	 * copying the selected plan.
	 * <BR>
	 * The idea behind this restriction is to avoid inconsistencies, as, contrary
	 * to an individual plan like {@link PlanImpl}, a {@link JointPlan} is tied to
	 * a specific clique.
	 *
	 * @deprecated
	 * @throws UnsupportedOperationException
	 */
	@Override
	public boolean addPlan(Plan p) {
		//cast to joint plan
		//check clique consistency
		throw new UnsupportedOperationException("using yet unimplemented clique.addPlan method:");
	}

	/**
	 * @return the selected plan of the clique.
	 */
	@Override
	public Plan getSelectedPlan() {
		return this.selectedPlan;
	}

	/**
	 * @return the id of the clique
	 */
	@Override
	public Id getId() {
		return this.id;
	}

	/**
	 * @return null
	 */
	@Override
	public Map<String,Object> getCustomAttributes() {
		return null;
	}

	/*
	 * ========================================================================
	 * methods existing for PersonImpl but not in the interface
	 * ========================================================================
	 */
	/**
	 * Sets the selected plan of the clique to the given plan. The plan must
	 * already be a plan of the clique, and will not be added.
	 *
	 * @param selectedPlan the plan to mark as selected.
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

	/**
	 * sets the seleted plan if it is a {@link JointPlan}
	 * 
	 * @throws IllegalArgumentException if the plan cannot be cast to a
	 * {@link JointPlan}
	 */
	public final void setSelectedPlan(final Plan selectedPlan) {
		JointPlan plan; 

		try {
			plan = (JointPlan) selectedPlan;
		}
		catch (ClassCastException e) {
			throw new IllegalArgumentException("cannot select a non joint plan for a clique", e);
		}
		this.setSelectedPlan(plan);
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
	/**
	 * @return the internal members map.
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

	/**
	 * Removes the plan from the clique database, and removes the corresponding
	 * individual plans at the individual level.
	 *
	 * @return true if the plan was removed, false otherwise.
	 * @throws RuntimeException if the plan was properly removed at the global level
	 * but not at the individual one.
	 */
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
	 * Builds a joint plan based on the plans of the members.
	 * To call immediately after having added all members.
	 * <BR>
	 * If the individual plan types allow it, all individual plans are kept.
	 * Otherwise, the selected plans of the individuals are grouped in a joint plan,
	 * the other plans are discarded.
	 * <BR>
	 * If the plan type identify joint plans, the plans are assumed synchronized.
	 * Otherwise, the plans are synchronised at construction.
	 * <BR><BR>
	 * CAUTION: importing from the plan dump will produce valid plans, but not a strict
	 * copy of the imported plans: the leg ids, that are used to resolve linked legs,
	 * are not dumped. This does not currently pose problems, but could if the
	 * cliques where to maintain several "father" plans.
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
						if (warnKeepOnlySelected) {
							log.warn("only keeping the selected plan for agent"+
									" with multiple plans. Message given only once.");
							warnKeepOnlySelected = false;
						}
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
								if (selected.equals(defaultType)) {
									selected = currentPlan.getType();
								}
								else if (!selected.equals(currentPlan.getType())) {
									throw new RuntimeException("different plans are selected at the individual level.");
								}
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

