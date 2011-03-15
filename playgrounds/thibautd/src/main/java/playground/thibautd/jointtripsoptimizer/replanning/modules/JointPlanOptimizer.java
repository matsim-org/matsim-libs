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

import java.util.Random;

import org.apache.log4j.Logger;

import org.jgap.Genotype;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * @author thibautd
 */
public class JointPlanOptimizer implements PlanAlgorithm {
	private static final Logger log = Logger.getLogger(JointPlanOptimizer.class);

	private final ScoringFunctionFactory fitnessFunctionFactory;
	private final JointReplanningConfigGroup configGroup;

	private final Random randomGenerator = MatsimRandom.getLocalInstance();

	public JointPlanOptimizer(
			ScoringFunctionFactory scoringFunctionFactory,
			JointReplanningConfigGroup configGroup
			) {
		this.fitnessFunctionFactory = scoringFunctionFactory;
		this.configGroup = configGroup;
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
		JointPlanOptimizerJGAPConfiguration jgapConfig =
			new JointPlanOptimizerJGAPConfiguration(plan, this.configGroup,
					this.randomGenerator.nextLong());

		JointPlanOptimizerPopulationFactory populationFactory =
			new JointPlanOptimizerPopulationFactory(jgapConfig);

		//TODO: set fitness function
		Genotype gaPopulation = populationFactory.createRandomInitialGenotype();

		//TODO: choose between a fixed number of iterations of an evolution monitor
		gaPopulation.evolve(this.configGroup.getMaxIterations());

		//TODO: get fittest chromosome, and modify the given plan accordingly
	}
}

