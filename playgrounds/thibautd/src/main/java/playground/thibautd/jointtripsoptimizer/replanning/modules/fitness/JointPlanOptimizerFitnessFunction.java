/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerFitnessFunction.java
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
//package playground.thibautd.jointtripsoptimizer.replanning.modules.fitness;
//
//import org.jgap.FitnessFunction;
//import org.jgap.IChromosome;
//
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.api.core.v01.population.Activity;
//import org.matsim.api.core.v01.population.Leg;
//import org.matsim.api.core.v01.population.Plan;
//import org.matsim.api.core.v01.population.PlanElement;
//import org.matsim.core.router.PlansCalcRoute;
//import org.matsim.core.scoring.ScoringFunction;
//import org.matsim.core.scoring.ScoringFunctionFactory;
//
//import playground.thibautd.jointtripsoptimizer.population.JointPlan;
//import playground.thibautd.jointtripsoptimizer.replanning.modules.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;
//import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerDecoder;
//import playground.thibautd.jointtripsoptimizer.replanning.modules.pipeddecoder.JointPlanOptimizerDecoderFactory;
//import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;
//
///**
// * A fitness function which uses a {@link JointPlanOptimizerDecoderFactory}
// * decoder and scores the resulting plan.
// *
// * @author thibautd
// */
//public class JointPlanOptimizerFitnessFunction extends AbstractJointPlanOptimizerFitnessFunction {
//
//	private static final long serialVersionUID = 1L;
//
//	/**
//	 * replacement for super.m_lastComputedFitnessValue, to keep the
//	 * "getlastfitnessvalue" functionnality
//	 */
//	private double lastComputedFitnessValue;
//
//	private final JointPlanOptimizerDecoder decoder;
//	private final ScoringFunctionFactory scoringFunctionFactory;
//
//	public JointPlanOptimizerFitnessFunction(
//			final JointPlan plan,
//			final JointReplanningConfigGroup configGroup,
//			final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
//			final PlansCalcRoute routingAlgorithm,
//			final Network network,
//			final int numJointEpisodes,
//			final int numEpisodes,
//			final int nMembers,
//			final ScoringFunctionFactory scoringFunctionFactory) {
//		super();
//		this.decoder = (new JointPlanOptimizerDecoderFactory(plan, configGroup, legTravelTimeEstimatorFactory,
//				routingAlgorithm, network, numJointEpisodes, numEpisodes, nMembers)).createDecoder();
//		this.scoringFunctionFactory = scoringFunctionFactory;
//	}
//
//	@Override
//	protected double evaluate(final IChromosome chromosome) {
//		JointPlan plan = this.decoder.decode(chromosome);
//		double score = this.getScore(plan);
//		return score;
//	}
//
//	private double getScore(final JointPlan plan) {
//		ScoringFunction fitnessFunction;
//		Activity currentActivity;
//		Leg currentLeg;
//		double now;
//
//		for (Plan indivPlan : plan.getIndividualPlans().values()) {
//			fitnessFunction =
//				this.scoringFunctionFactory.createNewScoringFunction(indivPlan);
//			now = 0d;
//	
//			// step through plan and score it
//			for (PlanElement pe : indivPlan.getPlanElements()) {
//				if (pe instanceof Activity) {
//					currentActivity = (Activity) pe;
//					fitnessFunction.startActivity(now, currentActivity);
//					now = currentActivity.getEndTime();
//					fitnessFunction.endActivity(now, currentActivity);
//				}
//				else if (pe instanceof Leg) {
//					currentLeg = (Leg) pe;
//					now = currentLeg.getDepartureTime();
//					fitnessFunction.startLeg(now, currentLeg);
//					now = currentLeg.getDepartureTime() + currentLeg.getTravelTime();
//					fitnessFunction.endLeg(now);
//				}
//				else {
//					throw new IllegalArgumentException("unrecognized plan element type");
//				}
//			}
//
//			fitnessFunction.finish();
//			indivPlan.setScore(fitnessFunction.getScore());
//		}
//
//		return plan.getScore();
//	}
//
//	public JointPlanOptimizerDecoder getDecoder() {
//		return this.decoder;
//	}
//
//	/**
//	 * Reimplements the jgap default by allowing a negative fitness.
//	 */
//	@Override
//	public double getFitnessValue(final IChromosome a_subject) {
//		double fitnessValue = evaluate(a_subject);
//		this.lastComputedFitnessValue = fitnessValue;
//		return fitnessValue;
//	}
//
//	@Override
//	public double getLastComputedFitnessValue() {
//		return this.lastComputedFitnessValue;
//	}
//}
//
