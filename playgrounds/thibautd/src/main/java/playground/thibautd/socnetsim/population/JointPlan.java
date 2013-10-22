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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import playground.thibautd.socnetsim.scoring.ScoresAggregator;

/**
 * class for handling synchronized plans.
 * @author thibautd
 */
public class JointPlan {
	private final Map<Id,Plan> individualPlans = new LinkedHashMap<Id,Plan>();

	private ScoresAggregator aggregator;

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

	/**
	 * Returns the global score as defined by the score aggregator
	 */
	public Double getScore() {
		return this.aggregator.getJointScore( individualPlans.values() );
	}

	/*
	 * =========================================================================
	 * JointPlan specific methods
	 * =========================================================================
	 */
	public Plan getIndividualPlan(final Id id) {
		return this.individualPlans.get(id);
	}

	public Map<Id,Plan> getIndividualPlans() {
		return this.individualPlans;
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

	@Override
	public String toString() {
		return getClass().getSimpleName()+": plans="+getIndividualPlans();
			//", isSelected="+this.isSelected();
	}

	public ScoresAggregator getScoresAggregator() {
		return this.aggregator;
	}
}
