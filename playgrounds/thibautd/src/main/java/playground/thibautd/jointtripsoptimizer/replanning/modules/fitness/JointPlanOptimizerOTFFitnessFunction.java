/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerOTFFitnessFunction.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules.fitness;

import org.jgap.IChromosome;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.replanning.modules.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerDecoder;
import playground.thibautd.jointtripsoptimizer.replanning.modules.pipeddecoder.DurationOnTheFlyScorer;
import playground.thibautd.jointtripsoptimizer.replanning.modules.pipeddecoder.DurationSimplexAlgo;
import playground.thibautd.jointtripsoptimizer.replanning.modules.pipeddecoder.FinalScorer;
import playground.thibautd.jointtripsoptimizer.replanning.modules.pipeddecoder.JointPlanOptimizerDecoderFactory;
import playground.thibautd.jointtripsoptimizer.replanning.modules.pipeddecoder.JointPlanOptimizerPartialDecoderFactory;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Implementation of a scoring function that scores durations "on the fly" rather
 * than on a decoded JointPlan instance.
 *
 * @author thibautd
 */
public class JointPlanOptimizerOTFFitnessFunction extends AbstractJointPlanOptimizerFitnessFunction {

	private static final long serialVersionUID = 1L;

	/**
	 * replacement for super.m_lastComputedFitnessValue, to keep the
	 * "getlastfitnessvalue" functionnality
	 */
	private double lastComputedFitnessValue;

	private final JointPlanOptimizerDecoder decoder;
	private final JointPlanOptimizerDecoder fullDecoder;
	//private final DurationOnTheFlyScorer scorer;
	private final FinalScorer scorer;

	public JointPlanOptimizerOTFFitnessFunction(
			final JointPlan plan,
			final JointReplanningConfigGroup configGroup,
			final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
			final PlansCalcRoute routingAlgorithm,
			final Network network,
			final int numJointEpisodes,
			final int numEpisodes,
			final int nMembers,
			final boolean isMemetic,
			final ScoringFunctionFactory scoringFunctionFactory) {
		this(plan,
			configGroup,
			legTravelTimeEstimatorFactory,
			routingAlgorithm,
			network,
			numJointEpisodes,
			numEpisodes,
			nMembers,
			isMemetic,
			scoringFunctionFactory,
			(new JointPlanOptimizerPartialDecoderFactory(
				plan,
				configGroup,
				numJointEpisodes,
				numEpisodes)).createDecoder(),
			(new JointPlanOptimizerDecoderFactory(
				plan,
				configGroup,
				legTravelTimeEstimatorFactory,
				routingAlgorithm,
				network,
				numJointEpisodes,
				numEpisodes,
				nMembers)).createDecoder());
	}

	public JointPlanOptimizerOTFFitnessFunction(
			final JointPlan plan,
			final JointReplanningConfigGroup configGroup,
			final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
			final PlansCalcRoute routingAlgorithm,
			final Network network,
			final int numJointEpisodes,
			final int numEpisodes,
			final int nMembers,
			final boolean isMemetic,
			final ScoringFunctionFactory scoringFunctionFactory,
			final JointPlanOptimizerDecoder partialDecoder,
			final JointPlanOptimizerDecoder fullDecoder) {
		super();
		this.decoder = partialDecoder;
		this.fullDecoder = fullDecoder;

		if (isMemetic) {
			this.scorer = new DurationSimplexAlgo(new DurationOnTheFlyScorer(
						plan,
						configGroup,
						scoringFunctionFactory,
						legTravelTimeEstimatorFactory,
						routingAlgorithm,
						network,
						numJointEpisodes,
						numEpisodes,
						nMembers));
		}
		else {
			this.scorer =new DurationOnTheFlyScorer(
						plan,
						configGroup,
						scoringFunctionFactory,
						legTravelTimeEstimatorFactory,
						routingAlgorithm,
						network,
						numJointEpisodes,
						numEpisodes,
						nMembers);
		}
	}


	@Override
	protected double evaluate(final IChromosome chromosome) {
		JointPlan plan = this.decoder.decode(chromosome);
		return this.scorer.score(chromosome, plan);
	}

	public JointPlanOptimizerDecoder getDecoder() {
		return this.fullDecoder;
	}

	/**
	 * Reimplements the jgap default by allowing a negative fitness.
	 */
	@Override
	public double getFitnessValue(final IChromosome a_subject) {
		double fitnessValue = evaluate(a_subject);
		this.lastComputedFitnessValue = fitnessValue;
		return fitnessValue;
	}

	@Override
	public double getLastComputedFitnessValue() {
		return this.lastComputedFitnessValue;
	}
}

