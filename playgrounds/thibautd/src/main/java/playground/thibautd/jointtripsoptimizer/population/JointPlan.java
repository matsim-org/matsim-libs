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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

/**
 * class for handling synchronized plans.
 * It implements the plan interface to be compatible with the StrategyManager.
 * @author thibautd
 */
public class JointPlan implements Plan {

	private final Map<Id,Plan> individualPlans = new HashMap<Id,Plan>();
	/**
	 * for robust resolution of links between activities.
	 */
	private final Map<IdLeg, JointLeg> legsMap = new TreeMap<IdLeg, JointLeg>();
	/**
	 * true if the individual plans are maintained at the individual level.
	 */
	private final boolean setAtIndividualLevel;

	private final Clique clique;

	//private Id currentIndividual = null;
	//private Iterator<Id> individualsIterator;

	/**
	 * Creates a joint plan from individual plans.
	 * The plans are added at the individual level.
	 * equivalent to JointPlan(clique, plans, true).
	 */
	public JointPlan(
			Clique clique,
			Map<Id, ? extends Plan> plans) {
		this(clique, plans, true);
	}

	/**
	 * Creates a joint plan from individual plans.
	 * Two individual trips to be shared must have their Pick-Up activity type set
	 * to 'pu_i', where i is an integer which identifies the joint trip.
	 * @param addAtIndividualLevel if true, the plans are added to the Person's plans.
	 * set to false for a temporary plan (in a replanning for example).
	 */
	//TODO: improve:
	// - less hard-coded modes
	// - improve reimplacement leg creation
	public JointPlan(
			Clique clique,
			Map<Id, ? extends Plan> plans,
			boolean addAtIndividualLevel) {
		this.setAtIndividualLevel = addAtIndividualLevel;
		Plan currentPlan;
		this.clique = clique;

		Map<String, List<JointLeg>> toLink = new HashMap<String, List<JointLeg>>();
		String actType;
		String currentJointEpisodeId = null;
		JointLeg currentLeg;
		JointActivity currentActivity;

		//TODO: check for consistency (referenced IDs, etc)
		for (Id id: plans.keySet()) {
			currentPlan = new PlanImpl(this.clique.getMembers().get(id));

			for (PlanElement pe : plans.get(id).getPlanElements()) {

				if (pe instanceof Activity) {
					currentActivity = new JointActivity((Activity) pe, 
							this.clique.getMembers().get(id));
					actType = currentActivity.getType();
					if (actType.matches(JointActingTypes.PICK_UP_REGEXP)) {
						currentJointEpisodeId =
							actType.split(JointActingTypes.PICK_UP_SPLIT_EXPR)[1];
						currentActivity.setType(JointActingTypes.PICK_UP);
					}
					currentPlan.addActivity(currentActivity);

				} else {
					currentLeg = new JointLeg((Leg) pe,
							(Person) this.clique.getMembers().get(id));
					if (currentJointEpisodeId != null) {
						// this leg is a shared leg, remember this.
						if (!toLink.containsKey(currentJointEpisodeId)) {
							toLink.put(currentJointEpisodeId, new ArrayList<JointLeg>());
						}
						toLink.get(currentJointEpisodeId).add(currentLeg);
						currentJointEpisodeId = null;
					}
					currentPlan.addLeg(currentLeg);
				}
			}
			this.individualPlans.put(id, currentPlan);

			if (addAtIndividualLevel) {
				this.clique.getMembers().get(id).addPlan(currentPlan);
			}
		}

		// create the links that where encoded in the activity types names
		// TODO: improve the way reimplacement modes are determined.
		for (List<JointLeg> legsToLink : toLink.values()) {
			for (JointLeg leg : legsToLink) {
				if (leg.getMode().equals(TransportMode.car)) {
					leg.setIsDriver(true);
				//	leg.setAssociatedIndividualLeg(
				//			new JointLeg(leg.getMode(), leg.getPerson()));
				//} else {
				//	leg.setAssociatedIndividualLeg(
				//			new JointLeg(TransportMode.pt, leg.getPerson()));
				}
				for (JointLeg linkedLeg : legsToLink) {
					if (leg != linkedLeg) {
						leg.addLinkedElementById(linkedLeg.getId());
					}
				}	
			}
		}

		this.constructLegsMap();
	}

	public JointPlan(JointPlan plan) {
		this(plan.getClique(), plan.getIndividualPlans());
	}

	private void constructLegsMap() {
		IdLeg currentLegId;

		this.legsMap.clear();
		for (PlanElement pe : this.getPlanElements()) {
			if (pe instanceof JointLeg) {
				((JointLeg) pe).setJointPlan(this);
				currentLegId = ((JointLeg) pe).getId();
				if (!this.legsMap.keySet().contains(currentLegId)) {
					this.legsMap.put(currentLegId, (JointLeg) pe);
				} else {
					throw new IllegalArgumentException("duplicate id found during"
							+" JointPlan construction");
				}
			}
		}
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
		//log.warn("using addLeg() on JointPlan: make sure current individual"+
		//		" is used correctly!");
		//if (leg instanceof JointLeg) {
		//	this.getIndividualPlan(this.getCurrentIndividual()).addLeg(
		//			(JointLeg) leg);
		//} else {
		//	throw new IllegalArgumentException("trying to add a non-joint"+
		//			"leg to joint plan: failed.");
		//}
		throw new UnsupportedOperationException();
	}

	/**
	 * Adds a (joint) activity to the plan.
	 * Members of the clique must be set before runing this method.
	 * @param act a JointActivity object, with the participants belonging to the
	 * clique.
	 */
	@Override
	public void addActivity(Activity act) {
		//log.warn("using addActivity() on JointPlan: make sure current individual"+
		//		" is used correctly!");
		//if (act instanceof JointActivity) {
		//	this.getIndividualPlan(this.getCurrentIndividual()).addActivity(
		//			(JointActivity) act);
		//} else {
		//	throw new IllegalArgumentException("trying to add a non-joint"+
		//			"activity to joint plan: failed.");
		//}
		throw new UnsupportedOperationException();
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
		// do not log warning (used at each iteration in the strategy manager
		// => too verbose
		// log.warn("using getPerson to get clique from JointPlan instance.");
		return this.getClique();
	}

	/**
	 * XXX unsupported: risk of breaking link individual plans/clique is too high
	 * @param person a Clique to be passed to setClique.
	 */
	@Override
	public void setPerson(Person person) {
		//log.warn("using setPerson to set clique from JointPlan instance.");
		//try{
		//	this.setClique((Clique) person);
		//} catch (java.lang.ClassCastException e) {
		//	throw new IllegalArgumentException("unable to set "+person+" in JointPlan: is not a clique!");
		//}
		throw new UnsupportedOperationException("JointPlan instances can only be"
				+" associated to a clique at construction");
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

	///**
	// * Sets the clique associated to the joint plan and sets the current
	// * individual to the first member of the clique.
	// * May be moved to a constructor in the future (final clique field).
	// * XXX do not use, breaks the link individual plans/clique!
	// */
	//public void setClique(Clique clique) {
	//	this.clique = clique;
	//	this.resetCurrentIndividual();
	//}
	
	public Map<Id, List<PlanElement>> getIndividualPlanElements() {
		Map<Id, List<PlanElement>> output = new TreeMap<Id, List<PlanElement>>();

		for (Map.Entry<Id, Plan> entry : this.individualPlans.entrySet()) {
			output.put(entry.getKey(), entry.getValue().getPlanElements());
		}

		return output;
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

	//public void resetCurrentIndividual() {
	//	this.individualsIterator = this.clique.getMembers().keySet().iterator();
	//	this.currentIndividual = this.individualsIterator.next();
	//}

	///**
	// * Jumps to the next individual.
	// * If current individual is the last individual, returns false.
	// */
	//public boolean nextIndividual() {
	//	//TODO
	//	if (this.individualsIterator.hasNext()) {
	//		this.currentIndividual = this.individualsIterator.next();
	//		return true;
	//	}
	//	return false;
	//}

	//public Id getCurrentIndividual() {
	//	return this.currentIndividual;
	//}

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
			throw new UnsupportedOperationException("resetting a joint plan from"+
					" a plan of a different clique is unsupported.");
		}
		if (this.setAtIndividualLevel) {
			for (Person currentIndividual : this.clique.getMembers().values()) {
				// remove the corresponding plan at the individual level
				currentIndividual.getPlans().remove(
						this.individualPlans.get(currentIndividual.getId()));
				// replace it by the new plan
				currentIndividual.addPlan(plan.getIndividualPlan(currentIndividual));
				// set it as selected if it was the selected plan
				if (this.isSelected()) {
					// no possibility to set the selected plan with the Person interface
					((PersonImpl) currentIndividual).setSelectedPlan(
						plan.getIndividualPlan(currentIndividual));
				}
			}
		}
		this.individualPlans.clear();
		this.individualPlans.putAll(plan.individualPlans);
		this.legsMap.clear();
		this.legsMap.putAll(plan.legsMap);
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

	/**
	 * Returns the a leg given its Id.
	 * used to resolve links between joint legs.
	 * for used in JointLeg only.
	 */
	JointLeg getLegById(IdLeg legId) {
		if (!this.legsMap.containsKey(legId)) {
			throw new RuntimeException("legs links could not be resolved");
		}
		return this.legsMap.get(legId);
	}

	/**
	 * Returns the "type" of the plan.
	 * This allows to make sure that the most "general" plan will not be removed.
	 * @return a string, corresponding to a list of the ids of the shared legs,
	 * separated by "-". No shared leg corresponds to the type "".
	 */
	public String getType() {
		String type = "";
		boolean notFirst = false;

		for (JointLeg currentJointLeg : this.legsMap.values()) {
			if (currentJointLeg.getJoint()) {
				if (notFirst) {
					type += "-";
				} else {
					notFirst = true;
				}
				type += currentJointLeg.getId();
			}
		}

		return type;
	}
}

