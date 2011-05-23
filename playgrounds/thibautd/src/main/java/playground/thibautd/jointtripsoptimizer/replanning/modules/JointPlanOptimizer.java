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

import org.jgap.Genotype;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.replanning.modules.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * @author thibautd
 */
public class JointPlanOptimizer implements PlanAlgorithm {
	private final ScoringFunctionFactory fitnessFunctionFactory;
	private final JointReplanningConfigGroup configGroup;
	private final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory;
	private final PlansCalcRoute routingAlgorithm;
	private final Network network;
	private final String outputPath;

	private final Random randomGenerator = MatsimRandom.getLocalInstance();

	public JointPlanOptimizer(
			final JointReplanningConfigGroup configGroup,
			final ScoringFunctionFactory scoringFunctionFactory,
			final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
			final PlansCalcRoute routingAlgorithm,
			final Network network,
			final String iterationOutputPath
			) {
		this.fitnessFunctionFactory = scoringFunctionFactory;
		this.configGroup = configGroup;
		this.legTravelTimeEstimatorFactory = legTravelTimeEstimatorFactory;
		this.routingAlgorithm = routingAlgorithm;
		this.network = network;
		this.outputPath = iterationOutputPath;
	}

	@Override
	public void run(final Plan plan) {
		if (plan instanceof JointPlan) {
			//log.debug("joint plan optimization algorithm lanched succesfully");
			this.run((JointPlan) plan);
		} else {
			throw new IllegalArgumentException("JointPlanOptimizer launched with"+
					"a non-joint plan");
		}
	}

	/**
	 * the actual optimisation algorithm, operating on a joint plan.
	 */
	private final void run(final JointPlan plan) {
		if (!isOptimizablePlan(plan)) {
			return;
		}

		JointPlanOptimizerJGAPConfiguration jgapConfig =
			new JointPlanOptimizerJGAPConfiguration(
					plan,
					this.configGroup,
					this.fitnessFunctionFactory,
					this.legTravelTimeEstimatorFactory,
					this.routingAlgorithm,
					this.network,
					this.outputPath,
					this.randomGenerator.nextLong());

		JointPlanOptimizerPopulationFactory populationFactory =
			new JointPlanOptimizerPopulationFactory(jgapConfig);

		Genotype gaPopulation = populationFactory.createRandomInitialGenotype();

		if (this.configGroup.getFitnessToMonitor()) {
			//log.debug("monitoring fitness");
			gaPopulation.evolve(jgapConfig.getEvolutionMonitor());
		}
		else {
			gaPopulation.evolve(this.configGroup.getMaxIterations());
		}

		//get fittest chromosome, and modify the given plan accordingly
		JointPlan evolvedPlan = jgapConfig.getDecoder().decode(
				gaPopulation.getFittestChromosome());
				//((JointPlanOptimizerJGAPBreeder) jgapConfig.getBreeder()).getAllTimesBest());
		plan.resetFromPlan(evolvedPlan);
		plan.resetScores();
	}

	private boolean isOptimizablePlan(final JointPlan plan) {
		for (Plan indivPlan : plan.getIndividualPlans().values()) {
			if (indivPlan.getPlanElements().size() > 1) {
				return true;
			}
		}
		return false;
	}
}

