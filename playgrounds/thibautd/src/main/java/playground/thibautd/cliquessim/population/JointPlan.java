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
package playground.thibautd.cliquessim.population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.thibautd.cliquessim.scoring.HomogeneousScoreAggregator;
import playground.thibautd.cliquessim.scoring.ScoresAggregator;

/**
 * class for handling synchronized plans.
 * It implements the plan interface to be compatible with the StrategyManager.
 *
 * FIXME: currently, the JointPlan is responsible for "inserting" itself at
 * the individual level. This is quite messy, and should be moved at an upper level.
 * @author thibautd
 */
public class JointPlan implements Plan {
	private final Map<Id,Plan> individualPlans = new HashMap<Id,Plan>();
	private final boolean setAtIndividualLevel;

	private ScoresAggregator aggregator;

	/**
	 * Creates a joint plan from individual plans.
	 * The plans are added at the individual level.
	 * equivalent to JointPlan(clique, plans, true, true).
	 */
	public JointPlan(
			final Map<Id, ? extends Plan> plans) {
		this(plans, true);
	}

	/**
	 * equivalent to JointPlan(clique, plans, addAtIndividualLevel, true)
	 */
	public JointPlan(
			final Map<Id, ? extends Plan> plans,
			final boolean addAtIndividualLevel) {
		this( plans, addAtIndividualLevel, new HomogeneousScoreAggregator());
	}

	/**
	 * Creates a joint plan from individual plans.
	 * Two individual trips to be shared must have their Pick-Up activity type set
	 * to 'pu_i', where i is an integer which identifies the joint trip.
	 * @param plans the individual plans. If they consist of Joint activities, 
	 * those activities are referenced, otherwise, they are copied in a joint activity.
	 * @param addAtIndividualLevel if true, the plans are added to the Person's plans.
	 * set to false for a temporary plan (in a replaning for example).
	 */
	//TODO: separate in several helpers (too messy)
	public JointPlan(
			final Map<Id, ? extends Plan> plans,
			final boolean addAtIndividualLevel,
			final ScoresAggregator aggregator) {
		this.setAtIndividualLevel = addAtIndividualLevel;

		if (addAtIndividualLevel) {
			for (Plan plan : plans.values()) {
				Person person = plan.getPerson();
				if (!person.getPlans().contains( plan )) {
					person.addPlan( plan );
				}
			}
		}
		this.individualPlans.putAll( plans );
		this.aggregator = aggregator;
	}

	/**
	 * makes a <u>shallow</u> copy of the plan.
	 */
	public JointPlan(final JointPlan plan) {
		this(
				cloneIndividualPlans( plan ),
				plan.setAtIndividualLevel,
				plan.getScoresAggregator());
		//this.setJointTripPossibilities( plan.getJointTripPossibilities() );
	}

	private static Map<Id, Plan> cloneIndividualPlans(final JointPlan plan) {
		Map<Id , Plan> plans = new HashMap<Id, Plan>();

		for (Map.Entry<Id, Plan> indiv : plan.getIndividualPlans().entrySet()) {
			PlanImpl newPlan = new PlanImpl( indiv.getValue().getPerson() );
			newPlan.copyPlan( indiv.getValue() );
			plans.put( indiv.getKey() , newPlan );
		}
		
		return plans;
	}

	/*
	 * =========================================================================
	 * Plan interface methods
	 * =========================================================================
	 */
	/**
	 * @return the list of plan elements, for all individuals. While the plan 
	 * elements are internal references, the list is not, and is immutable.
	 */
	@Override
	public List<PlanElement> getPlanElements() {
		List<PlanElement> output = new ArrayList<PlanElement>();
		for (Plan plan : this.individualPlans.values()) {
			output.addAll(plan.getPlanElements());
		}
		return Collections.unmodifiableList(output);
	}


	/**
	 * Inherited from the interface, but unimplemented.
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void addLeg(final Leg leg) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Inherited from the interface, but unimplemented.
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void addActivity(final Activity act) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSelected() {
		int nsel = 0;
		int tot = 0;
		for (Plan plan : individualPlans.values()) {
			if (plan.isSelected()) nsel++;
			tot++;
		}

		if (nsel == 0) return false;
		if (nsel == tot) return true;

		throw new IllegalStateException( "various selection status in "+individualPlans );
	}

	@Override
	public void setScore(final Double score) {
		throw new UnsupportedOperationException("JointPlan.setScore(Double) is"+
				" unsupported. The scores must be set on the individual plans.");
	}

	/**
	 * Returns the global score as defined by the score aggregator
	 */
	@Override
	public Double getScore() {
		return this.aggregator.getJointScore( individualPlans.values() );
	}

	/**
	 * @return the Clique to wich the plan is affected (wrapper to getClique).
	 */
	@Override
	public Person getPerson() {
		throw new UnsupportedOperationException( "JointPlans have no Person" );
	}

	/**
	 * Inherited from interface, but unimplemented.
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void setPerson(final Person person) {
		throw new UnsupportedOperationException("JointPlans have no Person" );
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
	public Plan getIndividualPlan(final Person person) {
		return this.getIndividualPlan(person.getId());
	}

	public Plan getIndividualPlan(final Id id) {
		return this.individualPlans.get(id);
	}

	public Map<Id,Plan> getIndividualPlans() {
		return this.individualPlans;
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
	 */
	public String getType() {
		return "jointPlan";
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+": plans="+getIndividualPlans()+", addAtIndividualLevel="+
			setAtIndividualLevel+", isSelected="+this.isSelected();
	}

	public ScoresAggregator getScoresAggregator() {
		return this.aggregator;
	}
}
