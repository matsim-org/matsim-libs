/* *********************************************************************** *
 * project: org.matsim.*
 * HomogeneousJointActivityScoring.java
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
package playground.thibautd.jointtripsoptimizer.scoring;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.charyparNagel.ActivityScoringFunction;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.interfaces.ActivityScoring;
import org.matsim.core.scoring.interfaces.BasicScoring;

import playground.thibautd.jointtripsoptimizer.population.JointActivity;
import playground.thibautd.jointtripsoptimizer.population.JointPlan;

/**
 * Group scoring function corresponding to one "CharyparNagelScoringFunction"
 * per individual.
 * @author thibautd
 */
public class HomogeneousJointActivityScoring implements ActivityScoring, BasicScoring {
	private final Map<Id, ActivityScoringFunction> individualScoringFunctions;
	private final Set<Id> ids;
	private Id currentId;

	public HomogeneousJointActivityScoring(
			Plan plan,
			final PlanCalcScoreConfigGroup config
			) {

		if (plan instanceof JointPlan) {
			JointPlan jointPlan = (JointPlan) plan;

			this.individualScoringFunctions = new HashMap<Id, ActivityScoringFunction>();
			this.ids = jointPlan.getClique().getMembers().keySet();

			for (Id id : this.ids) {
				individualScoringFunctions.put(id, new ActivityScoringFunction(
							jointPlan.getIndividualPlan(id),
							new CharyparNagelScoringParameters(config)));
			}
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public void finish() {
		for (Id id : this.ids) {
			individualScoringFunctions.get(id).finish();
		}
	}

	/**
	 * Simply return the sum of individual scores ("homogeneous" score)
	 */
	@Override
	public double getScore() {
		Double score = 0.0;

		for (Id id : this.ids) {
			score += individualScoringFunctions.get(id).getScore();
		}

		return score;
	}

	@Override
	public void reset() {
		for (Id id : this.ids) {
			individualScoringFunctions.get(id).reset();
		}	
	}

	@Override
	public void startActivity(double time, Activity act) {
		this.currentId = ((JointActivity) act).getPerson().getId();
		individualScoringFunctions.get(this.currentId).startActivity(time, act);
	}

	/**
	 * sets the end of the activity for the last activity started.
	 */
	@Override
	public void endActivity(double time) {
		individualScoringFunctions.get(this.currentId).endActivity(time);
	}
}

