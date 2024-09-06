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

package org.matsim.core.replanning.selectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.core.gbl.MatsimRandom;

/**
 * Changes to another plan if that plan is better.  Probability to change depends on score difference.
 *
 * @author kn based on mrieser
 */
public final class ExpBetaPlanChanger<T extends BasicPlan, I> implements PlanSelector<T, I> {
	private static final Logger log = LogManager.getLogger(ExpBetaPlanChanger.class);

	public static final class Factory<T extends BasicPlan,I> {
		private double beta = 1.;
		public ExpBetaPlanChanger<T,I> build() {
			return new ExpBetaPlanChanger<>( beta );
		}
		public Factory<T,I> setBetaValue( double beta ){
			this.beta = beta;
			return this;
		}
	}

	private final double beta;
	static boolean betaWrnFlag = true ;
	static boolean scoreWrnFlag = true ;


	/**
	 * @deprecated -- use {@link Factory}
	 */
	public ExpBetaPlanChanger(double beta) {
		this.beta = beta;
	}

	/**
	 * Changes to another plan with a probability proportional to exp( Delta scores ).
	 * Need to think through if this goes to Nash Equilibrium or to SUE !!!
	 */
	@Override
	public T selectPlan(final HasPlansAndId<T, I> person) {
		// current plan and random plan:
		T currentPlan = person.getSelectedPlan();
		T otherPlan = new RandomPlanSelector<T, I>().selectPlan(person);

		if (currentPlan == null) {
			// this case should only happen when the agent has no plans at all
			return null;
		}

		if ((currentPlan.getScore() == null) || (otherPlan.getScore() == null)) {
			/* With the previous behavior, Double.NaN was returned if no score was available.
			 * This resulted in weight=NaN below as well, and then ultimately in returning
			 * the currentPlan---what we're doing right now as well.
			 */
			if ( currentPlan.getScore()!=null && otherPlan.getScore()==null ) {
				if ( scoreWrnFlag ) {
					log.error( "yyyyyy not switching to other plan although it needs to be explored.  "
							+ "Possibly a serious bug; ask kai if you encounter this.  kai, sep'10" ) ;
					scoreWrnFlag = false ;
				}
			}
			return currentPlan;
		}

		// defending against NaN (which should not happen, but happens):
		if ( currentPlan.getScore().isNaN() ) {
			return otherPlan ;
		}
		if ( otherPlan.getScore().isNaN() ) {
			return currentPlan ;
		}

		double currentScore = currentPlan.getScore();
		double otherScore = otherPlan.getScore();

		if ( betaWrnFlag ) {
			log.warn("Would make sense to revise this once more.  See comments in code.  kai, nov08") ;
			/*   Gunnar says, rightly I think, that what is below hits the "0.01*weight > 1" threshold fairly quickly.
			 *   An alternative might be to divide by exp(0.5*beta*oS)+exp(0.5*beta*cS), or the max of these two numbers.  But:
			 *   (1) someone would need to go through the theory to make sure that we remain within what we have said before
			 *       (convergence to logit and proba of jump between equal options = 0.01
			 *   (2) someone would need to test if the "traffic" results are similar
			 */
			betaWrnFlag = false ;
		}
		double weight = Math.exp( 0.5 * this.beta * (otherScore - currentScore) );
		// (so far, this is >1 if otherScore>currentScore, and <=1 otherwise)
		// (beta is the slope (strength) of the operation: large beta means strong reaction)

		if (MatsimRandom.getRandom().nextDouble() < 0.01*weight ) { // as of now, 0.01 is hardcoded (proba to change when both
			                                           // scores are the same)
			return otherPlan;
		}
		return currentPlan;
	}

}
