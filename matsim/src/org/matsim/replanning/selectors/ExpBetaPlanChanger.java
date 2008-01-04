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

import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;

/**
 * Changes to another plan if that plan is better.  Probability to change depends on score difference.
 *
 * @author kn based on mrieser
 */
public class ExpBetaPlanChanger implements PlanSelectorI {

	private final double beta;

	public ExpBetaPlanChanger() {
		this.beta = Double.parseDouble(Gbl.getConfig().getParam("planCalcScore", "BrainExpBeta"));
	}

	/**
	 * Changes to another plan with a probability proportional to exp( Delta scores ).
	 * Need to think through if this goes to Nash Equilibrium or to SUE !!!
	 */
	public Plan selectPlan(final Person person) {
		// current plan and random plan:
		Plan currentPlan = person.getSelectedPlan();
		Plan otherPlan = person.getRandomPlan();

		double currentScore = currentPlan.getScore();
		double otherScore = otherPlan.getScore();

		double weight = Math.exp( this.beta * (otherScore - currentScore) );
		// (so far, this is >1 if otherScore>currentScore, and <=1 otherwise)
		// (beta is the slope (strength) of the operation: large beta means strong reaction)

		if (Gbl.random.nextDouble() < 0.01*weight ) { // as of now, 0.01 is hardcoded (proba to change when both
			                                           // scores are the same)
			return otherPlan;
		}
		return currentPlan;
	}

}
