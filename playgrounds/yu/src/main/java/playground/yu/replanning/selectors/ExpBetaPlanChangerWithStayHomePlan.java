/* *********************************************************************** *
 * project: org.matsim.*
 * ExpBetaPlanChangerWithStayHomePlan.java
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

package playground.yu.replanning.selectors;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.yu.utils.StayHomePlan;

/**
 * should only be used for {@code Scenario} with {@code Population}, among which
 * each {@code Person} has one "stay home" {@code Plan}, and with purpose
 * filling the lack of traffic. This is a changed version of
 * {@code ExpBetaPlanChanger}: a. Plans with null scores can be preferentially
 * chosen; b. If there are NOT plans with null scores any more, the unique
 * "stay home" {@code Plan} of this {@code Person}can be chosen with probability
 * 1-f (f - the probability that any NOT "stay home" {@code Plan} can be
 * chosen).
 *
 * @author yu
 *
 */
public class ExpBetaPlanChangerWithStayHomePlan implements PlanSelector {
	private final ExpBetaPlanChanger delegate;
	private final double f, betaBrain;

	/**
	 * @param f
	 *            the probability that any NOT "stay home" {@code Plan} can be
	 *            chosen
	 */
	public ExpBetaPlanChangerWithStayHomePlan(double f) {
		this(2d, f);
	}

	public ExpBetaPlanChangerWithStayHomePlan(double betaBrain, double f) {
		this.betaBrain = betaBrain;
		delegate = new ExpBetaPlanChanger(this.betaBrain);
		this.f = f;
	}

	private void scoreStayHomePlan(Person person) {
		// calculate stay Home Plan score, in order to realize the choice
		// probability "1-f", Score_stayHome = 1/betaBrain *
		// ln[(1-f)/f*sigma[exp(betaBrain*Score_other)]]
		Plan stayHomePlan = null;
		double expBetaScoreSum = 0d;

		for (Plan plan : person.getPlans()) {

			if (!StayHomePlan.stayHome(plan)) {
				expBetaScoreSum += Math.exp(betaBrain * plan.getScore());
			} else {// stay home
				stayHomePlan = plan;
			}
		}
		if (stayHomePlan == null) {
			System.err.println("There are NOT \"stay home\" Plan of Person\t"
					+ person.getId() + "\t!!!");
			return;
		}
		stayHomePlan.setScore(Math.log(expBetaScoreSum * (1d - f) / f)
				/ betaBrain);// TODO too large exp(beta*score) ...
	}

	@Override
	public Plan selectPlan(Person person) {

		// preferentially choose NULL-score Plan
		for (Plan plan : person.getPlans()) {
			if (plan.getScore() == null) {
				return plan;
			}
		}

		// calculate stay Home Plan score, in order to realize the choice
		// probability "1-f", Score_stayHome = 1/betaBrain *
		// ln[(1-f)/f*sigma[exp(betaBrain*Score_other)]]
		scoreStayHomePlan(person);

		// original result from ExpBetaPlanChanger
		return delegate.selectPlan(person);
	}
}
