/* *********************************************************************** *
 * project: org.matsim.*
 * JPOForOptimization.java
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
package playground.thibautd.gaparamoptimizer;

import java.util.Random;

import org.apache.log4j.Logger;

import org.jgap.Genotype;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.replanning.modules.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPConfiguration;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerPopulationFactory;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Runs the joint plan optimizer and returns the best obtained score
 * @author thibautd
 */
public class JPOForOptimization {
	private static final Logger log =
		Logger.getLogger(JPOForOptimization.class);

	private final ScoringFunctionFactory fitnessFunctionFactory;
	private final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory;
	private final PlansCalcRoute routingAlgorithm;
	private final Network network;
	private final String outputPath;

	private final Random randomGenerator = MatsimRandom.getLocalInstance();

	public JPOForOptimization(
			final ScoringFunctionFactory scoringFunctionFactory,
			final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
			final PlansCalcRoute routingAlgorithm,
			final Network network,
			final String iterationOutputPath
			) {
		this.fitnessFunctionFactory = scoringFunctionFactory;
		this.legTravelTimeEstimatorFactory = legTravelTimeEstimatorFactory;
		this.routingAlgorithm = routingAlgorithm;
		this.network = network;
		this.outputPath = iterationOutputPath;
	}

	/**
	 * @return the average score per agent in the clique
	 */
	public final double run(
			final JointReplanningConfigGroup configGroup,
			final JointPlan plan) {

		JointPlanOptimizerJGAPConfiguration jgapConfig =
			new JointPlanOptimizerJGAPConfiguration(
					plan,
					configGroup,
					this.fitnessFunctionFactory,
					this.legTravelTimeEstimatorFactory,
					this.routingAlgorithm,
					this.network,
					this.outputPath,
					this.randomGenerator.nextLong());

		JointPlanOptimizerPopulationFactory populationFactory =
			new JointPlanOptimizerPopulationFactory(jgapConfig);

		Genotype gaPopulation = populationFactory.createRandomInitialGenotype();

		if (configGroup.getFitnessToMonitor()) {
			//log.debug("monitoring fitness");
			gaPopulation.evolve(jgapConfig.getEvolutionMonitor());
		}
		else {
			gaPopulation.evolve(configGroup.getMaxIterations());
		}

		//log.debug("best fitness: "+gaPopulation.getFittestChromosome().getFitnessValue());
		//log.debug("clique size: "+plan.getClique().getMembers().size());
		return gaPopulation.getFittestChromosome().getFitnessValue() /
			plan.getClique().getMembers().size();
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

