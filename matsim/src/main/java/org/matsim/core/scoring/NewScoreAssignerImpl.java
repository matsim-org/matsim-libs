
/* *********************************************************************** *
 * project: org.matsim.*
 * NewScoreAssignerImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.scoring;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;

import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;

class NewScoreAssignerImpl implements NewScoreAssigner {

	static private final Logger log = LogManager.getLogger(NewScoreAssignerImpl.class);

	private Map<Plan,Integer> msaContributions = new HashMap<>() ;
	private Integer scoreMSAstartsAtIteration;
	private final double learningRate;
	private final boolean explainScores;
	private double scoreSum = 0.0;
	private long scoreCount = 0;

	@Inject
	NewScoreAssignerImpl(ScoringConfigGroup scoringConfigGroup, ControllerConfigGroup controllerConfigGroup) {
		if (scoringConfigGroup.getFractionOfIterationsToStartScoreMSA()!=null ) {
			final int diff = controllerConfigGroup.getLastIteration() - controllerConfigGroup.getFirstIteration();
			this.scoreMSAstartsAtIteration = (int) (diff
					* scoringConfigGroup.getFractionOfIterationsToStartScoreMSA() + controllerConfigGroup.getFirstIteration());
		}
		learningRate = scoringConfigGroup.getLearningRate();
		explainScores = scoringConfigGroup.isWriteScoreExplanations();
	}

	public void assignNewScores(int iteration, ScoringFunctionsForPopulation scoringFunctionsForPopulation, Population population) {
		log.info("it: " + iteration + " msaStart: " + this.scoreMSAstartsAtIteration );

		StringBuilder explanation = new StringBuilder();

		for (Person person : population.getPersons().values()) {
			ScoringFunction sf = scoringFunctionsForPopulation.getScoringFunctionForAgent(person.getId());
			double score = sf.getScore();
			Plan plan = person.getSelectedPlan();
			Double oldScore = plan.getScore();

			if (explainScores) {
				explanation.setLength(0);
				sf.explainScore(explanation);
				plan.getAttributes().putAttribute(ScoringFunction.SCORE_EXPLANATION_ATTR, explanation.toString());
			}

			if (oldScore == null) {
				plan.setScore(score);
				if ( plan.getScore().isNaN() ) {
					log.warn("score is NaN; plan:" + plan.toString() );
				}
			} else {
				if ( this.scoreMSAstartsAtIteration == null || iteration < this.scoreMSAstartsAtIteration ) {
					final double newScore = this.learningRate * score + (1 - this.learningRate) * oldScore;
					if ( log.isTraceEnabled() ) {
						log.trace( " lrn: " + this.learningRate + " oldScore: " + oldScore + " simScore: " + score + " newScore: " + newScore );
					}
					plan.setScore(newScore);
					if ( plan.getScore().isNaN() ) {
						log.warn("score is NaN; plan:" + plan.toString()+" with lrn: " + this.learningRate + " oldScore: " + oldScore + " simScore: " + score + " newScore: " + newScore );
					}
				} else {
//					double alpha = 1./(this.iteration - this.scoreMSAstartsAtIteration + 1) ;
//					alpha *= scenario.getConfig().strategy().getMaxAgentPlanMemorySize() ; //(**)
//					if ( alpha>1 ) {
//						alpha = 1. ;
//					}

					Integer msaContribs = this.msaContributions.get(plan) ;
					if ( msaContribs==null ) {
						msaContribs = 0 ;
					}
					this.msaContributions.put(plan,msaContribs+1) ;
					double alpha = 1./(msaContribs+1) ;

					final double newScore = alpha * score + (1.-alpha) * oldScore;
					if ( log.isTraceEnabled() ) {
						log.trace( " alpha: " + alpha + " oldScore: " + oldScore + " simScore: " + score + " newScore: " + newScore );
					}
					plan.setScore( newScore ) ;
					if ( plan.getScore().isNaN() ) {
						log.warn("score is NaN; plan:" + plan.toString() );
					}
					/*
					// the above is some variant of MSA (method of successive
					// averages). It is not the same as MSA since
					// a plan is typically not scored in every iteration.
					// However, plans are called with rates, for example
					// only every 10th iteration. Yet, something like 1/(10x)
					// still diverges in the same way as 1/x
					// when integrated, so MSA should still converge to the
					// correct result. kai, oct'12
					// The above argument may be theoretically correct.  But something 9/10*old+1/10*new is too slow in practice.  Now
					// multiplying with number of plans (**) in hope that it is better.  (Where is the theory department?) kai, nov'13
					 * Looks to me like this is now truly MSA. kai, apr'15
					// yyyy this has never been tested with scenarios :-(  .  At least there is a test case.  kai, oct'12
					// (In the meantime, I have used it in certain of my own 1% runs, e.g. Ivory Coast.)
					 */
				}
			}

			this.scoreSum += score;
			this.scoreCount++;
		}
	}


}
