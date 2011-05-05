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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.yu.demandModifications.StayHomePlan;
import playground.yu.utils.container.CollectionMax;

/**
 * should only be used for {@code Scenario} with {@code Population}, among which
 * each {@code Person} has one "stay home" {@code Plan}, and with purpose
 * filling the lack of traffic. This is a changed version of
 * {@code ExpBetaPlanChanger}:
 * <P>
 * a. Plans with null scores can be preferentially chosen;
 * <P>
 * b. If there are NOT plans with null scores any more, the unique "stay home"
 * {@code Plan} of this {@code Person}can be chosen with probability 1-f.
 * <P>
 * (f - the probability that any NOT "stay home" {@code Plan} can be chosen).
 * <P>
 * But this score of "stay home" {@code Plan} will NOT be changed, only a
 * changed dummy score is used by selecting {@code Plan}
 *
 * @author yu
 *
 */
public class ExpBetaPlanChangerWithStayHomePlan implements PlanSelector {
	private static final Logger log = Logger
			.getLogger(ExpBetaPlanChangerWithStayHomePlan.class);
	private final double f, betaBrain;
	static boolean betaWrnFlag = true;
	static boolean scoreWrnFlag = true;

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
		this.f = f;
	}

	private Double getDummyStayHomePlanScore4ChoiceProb(Person person) {
		/*
		 * calculate stay Home Plan score, in order to realize the choice
		 * probability "1-f", U_stayHome = 1/betaBrain * ln[(1-f)/f *
		 * sigma[exp(betaBrain * (U_other-U_max)]] + U_max
		 */
		Plan stayHomePlan = null;
		double expBetaScoreDiffSum = 0d;
		List<Double> unStayHomeScores = new ArrayList<Double>();

		for (Plan plan : person.getPlans()) {

			if (!StayHomePlan.isAStayHomePlan(plan)) {
				// expBetaScoreSum += Math.exp(betaBrain * plan.getScore());
				unStayHomeScores.add(plan.getScore());
			} else {// stay home
				stayHomePlan = plan;
			}
		}

		if (stayHomePlan == null) {
			log.error("There are NOT \"stay home\" Plan of Person\t"
					+ person.getId() + "\t!!!");
			return null;
		}

		double Vmax = CollectionMax.getDoubleMax(unStayHomeScores);
		for (Double Vj : unStayHomeScores) {
			expBetaScoreDiffSum += Math.exp(betaBrain * (Vj - Vmax));
		}

		return Math.log((1d - f) / f * expBetaScoreDiffSum) / betaBrain + Vmax;
	}

	/**
	 * Changes to another plan with a probability proportional to exp( Delta
	 * scores ). Need to think through if this goes to Nash Equilibrium or to
	 * SUE !!!
	 * <P>
	 * A changed dummy score for "stay home" {@code Plan} will be used
	 */
	@Override
	public Plan selectPlan(final Person person) {
		// preferentially choose NULL-score Plan
		for (Plan plan : person.getPlans()) {
			if (plan.getScore() == null) {
				return plan;
			}
		}

		/**
		 * calculates stay Home {@code Plan score}, to realize the choice
		 * probability "1-f",
		 * <P>
		 * the probability that any NOT "stay home" {@code Plan} can be chosen
		 * <P>
		 * Score_stayHome = 1/betaBrain * ln[ (1-f)/f * sigma[exp(betaBrain *
		 * Score_other)]]
		 */
		Double stayHomeScore = getDummyStayHomePlanScore4ChoiceProb(person);

		// current plan and random plan:
		Plan currentPlan = person.getSelectedPlan();
		Plan otherPlan = ((PersonImpl) person).getRandomPlan();

		if (currentPlan == null) {
			// this case should only happen when the agent has no plans at all
			return null;
		}

		if (currentPlan.getScore() == null || otherPlan.getScore() == null) {
			/*
			 * With the previous behavior, Double.NaN was returned if no score
			 * was available. This resulted in weight=NaN below as well, and
			 * then ultimately in returning the currentPlan---what we're doing
			 * right now as well.
			 */
			if (currentPlan.getScore() != null && otherPlan.getScore() == null) {
				if (scoreWrnFlag) {
					log.error("yyyyyy not switching to other plan although it needs to be explored.  "
							+ "Possibly a serious bug; ask kai if you encounter this.  kai, sep'10");
					scoreWrnFlag = false;
				}
			}
			return currentPlan;
		}
		double currentScore = !StayHomePlan.isAStayHomePlan(currentPlan) ? currentPlan
				.getScore() : stayHomeScore;
		double otherScore = !StayHomePlan.isAStayHomePlan(currentPlan) ? otherPlan
				.getScore().doubleValue() : stayHomeScore;

		if (betaWrnFlag) {
			log.warn("Would make sense to revise this once more.  See comments in code.  kai, nov08");
			/***
			 * Gunnar says, rightly I think, that what is below hits the
			 * "0.01*weight > 1" threshold fairly quickly. An alternative might
			 * be to divide by exp(0.5*beta*oS)+exp(0.5*beta*cS), or the max of
			 * these two numbers. But: (1) someone would need to go through the
			 * theory to make sure that we remain within what we have said
			 * before (convergence to logit and proba of jump between equal
			 * options = 0.01 (2) someone would need to test if the "traffic"
			 * results are similar
			 */
			betaWrnFlag = false;
		}
		double weight = Math.exp(0.5 * betaBrain * (otherScore - currentScore));
		// (so far, this is >1 if otherScore>currentScore, and <=1 otherwise)
		// (beta is the slope (strength) of the operation: large beta means
		// strong reaction)

		if (MatsimRandom.getRandom().nextDouble() < 0.01 * weight) { // as of
																		// now,
																		// 0.01
																		// is
																		// hardcoded
																		// (proba
																		// to
																		// change
																		// when
																		// both
			// scores are the same)
			return otherPlan;
		}
		return currentPlan;
	}
}
