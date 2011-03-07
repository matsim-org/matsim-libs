/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizer.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules;

import org.apache.log4j.Logger;

import org.jgap.IChromosome;

import org.matsim.api.core.v01.population.Plan;

import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.jointtripsoptimizer.population.JointPlan;

/**
 * @author thibautd
 */
public class JointPlanOptimizer implements PlanAlgorithm {
	private static final Logger log = Logger.getLogger(JointPlanOptimizer.class);

	private ScoringFunctionFactory fitnessFunctionFactory = null;

	public JointPlanOptimizer(
			ScoringFunctionFactory scoringFunctionFactory
			) {
		this.fitnessFunctionFactory = scoringFunctionFactory;
	}

	@Override
	public void run(Plan plan) {
		if (plan instanceof JointPlan) {
			log.debug("joint plan optimization algorithm lanched succesfully");
			this.run((JointPlan) plan);
		} else {
			throw new IllegalArgumentException("JointPlanOptimizer launched with"+
					"a non-joint plan");
		}
	}

	/**
	 * the actual optimisation algorithm, operating on a joint plan.
	 */
	private void run(JointPlan plan) {
	}

	/**
	 * Function responsible for scoring the plan encoded by a Chromosome.
	 * It does the following:
	 * -it steps through the plans, modifying the time affectation so that all
	 *  required face to face meetings are possible;
	 * -plans are then stretched to 24h;
	 * -it modifies back the chromosome to reflect the necessary changes;
	 * -it sets the plan passed in argument to the resulting plan if doModify is true;
	 * -it scores this plan by passing it to a scoring function.
	 */
	protected double stepThroughPlan(
			boolean doModify,
			boolean doScore,
			IChromosome chromosome,
			JointPlan plan ) {
		ScoringFunction scoringFunction = null;

		if (!doModify) {
			// if there is no need to write back the plan, reference a new instance.
			plan = new JointPlan(plan);
		}

		if (doScore) {
			scoringFunction = this.fitnessFunctionFactory.createNewScoringFunction(plan);
		}

		return 0.0d;
	}
}

