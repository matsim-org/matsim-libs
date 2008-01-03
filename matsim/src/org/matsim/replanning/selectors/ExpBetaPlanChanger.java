/* *********************************************************************** *
 * project: org.matsim.*
 * ExpBetaPlanChanger.java
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

package org.matsim.replanning.selectors;

import org.matsim.basic.v01.BasicPlan;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;

/**
 * Changes to another plan if that plan is better.  Proba to change depends on score difference.
 * If there are unscored plans, a random one
 * of the unscored plans will be chosen (optimistic strategy).
 *
 * @author kn based on mrieser
 */
public class ExpBetaPlanChanger implements PlanSelectorI {

	private static double MIN_WEIGHT = Double.MIN_VALUE;
	private final double beta;

	public ExpBetaPlanChanger() {
		this.beta = Double.parseDouble(Gbl.getConfig().getParam("planCalcScore", "BrainExpBeta"));
	}

	/**
	 * Changes to another plan with a proba proportional to exp( Delta scores ).
	 * Need to think through if this goes to Nash Eq. or to SUE !!!
	 */
	public Plan selectPlan(final Person person) {
		// First check if there are any unscored plans
		Plan selectedPlan = selectUnscoredPlan(person);
		if (selectedPlan != null) {
			return selectedPlan;
		}
		// Okay, no unscored plans...
		
		// current plan and random plan:
		Plan currentPlan = person.getSelectedPlan();
		Plan otherPlan = person.selectRandomPlan() ;
		
		double currentScore = currentPlan.getScore();
		double otherScore = otherPlan.getScore() ;
		
		double weight = Math.exp( this.beta * (otherScore - currentScore) ) ;
		// (so far, this is >1 if otherScore>currentScore, and <=1 otherwise)
		// (beta is the slope (strength) of the operation: large beta means strong reaction)
		
		if ( Gbl.random.nextDouble() < 0.01*weight ) { // as of now, 0.01 is hardcoded (proba to change when both 
			                                           // scores are the same)
			return otherPlan ;
		} else {
			return currentPlan ;
		}
		
//		// this case should never happen, except a person has no plans at all.
//		return null;
	}

	/**
	 * Returns a plan with undefined score, chosen randomly among all plans
	 * of the person with undefined score.
	 *
	 * @param person
	 * @return a random plan with undefined score, or null if none such plan exists.
	 */
	private Plan selectUnscoredPlan(final Person person) {
		// TODO [kn] this exists now both inside ExpBetaPlanSelect and in ExpBetaPlanChange.  Should be moved to
		// ONE location, maybe "class Person".
		int cntUnscored = 0;
		for (Plan plan : person.getPlans()) {
			if (Plan.isUndefinedScore(plan.getScore())) {
				cntUnscored++;
			}
		}
		if (cntUnscored > 0) {
			// select one of the unscored plans
			int idxUnscored = Gbl.random.nextInt(cntUnscored);
			cntUnscored = 0;
			for (Plan plan : person.getPlans()) {
				if (Plan.isUndefinedScore(plan.getScore())) {
					if (cntUnscored == idxUnscored) {
						person.setSelectedPlan(plan);
						return plan;
					}
					cntUnscored++;
				}
			}
		}
		return null;
	}

	/**
	 * Calculates the weight of a single plan
	 *
	 * @param plan
	 * @param maxScore
	 * @return the weight of the plan
	 */
	private double calcPlanWeight(final Plan plan, final double maxScore) {
		double weight = Math.exp(this.beta * (plan.getScore() - maxScore));
		if (weight <= 0.0) weight = MIN_WEIGHT;
		return weight;
	}
}
