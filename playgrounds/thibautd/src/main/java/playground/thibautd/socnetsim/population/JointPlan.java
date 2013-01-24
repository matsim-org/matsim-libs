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
package playground.thibautd.socnetsim.population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import playground.thibautd.socnetsim.scoring.ScoresAggregator;

/**
 * class for handling synchronized plans.
 * It implements the plan interface to be compatible with the StrategyManager.
 * @author thibautd
 */
public class JointPlan implements Plan {
	private final Map<Id,Plan> individualPlans = new LinkedHashMap<Id,Plan>();

	private ScoresAggregator aggregator;

	/**
	 * Creates a joint plan from individual plans.
	 * Two individual trips to be shared must have their Pick-Up activity type set
	 * to 'pu_i', where i is an integer which identifies the joint trip.
	 * @param plans the individual plans. If they consist of Joint activities, 
	 * those activities are referenced, otherwise, they are copied in a joint activity.
	 * @param addAtIndividualLevel if true, the plans are added to the Person's plans.
	 * set to false for a temporary plan (in a replaning for example).
	 */
	JointPlan(
			final Map<Id, ? extends Plan> plans,
			final ScoresAggregator aggregator) {
		this.individualPlans.putAll( plans );
		this.aggregator = aggregator;
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
		return getClass().getSimpleName()+": plans="+getIndividualPlans()+
			", isSelected="+this.isSelected();
	}

	public ScoresAggregator getScoresAggregator() {
		return this.aggregator;
	}
}
