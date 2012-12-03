/* *********************************************************************** *
 * project: org.matsim.*
 * JointTimeModeChooserConfigBuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.cliquessim.replanning.modules.jointtimemodechooser;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.thibautd.cliquessim.config.JointTimeModeChooserConfigGroup;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.tsplanoptimizer.framework.CompositeMoveGenerator;
import playground.thibautd.tsplanoptimizer.framework.CompositeTabuChecker;
import playground.thibautd.tsplanoptimizer.framework.EvolutionPlotter;
import playground.thibautd.tsplanoptimizer.framework.FitnessFunction;
import playground.thibautd.tsplanoptimizer.framework.ImprovementDelayMonitor;
import playground.thibautd.tsplanoptimizer.framework.NullMoveChecker;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.TabuSearchConfiguration;
import playground.thibautd.tsplanoptimizer.timemodechooser.DirectionTabuList;
import playground.thibautd.tsplanoptimizer.timemodechooser.FixedStepsIntegerMovesGenerator;

/**
 * @author thibautd
 */
class JointTimeModeChooserConfigBuilder {
	// the mode optimisation is inconsistent...
	private static final int N_ITER = 1000;
	private static final List<Integer> RESTRICTED_STEPS = Arrays.asList( 60 , 300 , 1500 );

	private final JointPlan plan;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final String outputDir;
	private final JointTimeModeChooserConfigGroup config;
	private final Random random;

	/**
	 * Creates a builder for optimisation without debuging output
	 * @param plan
	 * @param scoringFunctionFactory
	 * @param tripRouterFactory
	 */
	public JointTimeModeChooserConfigBuilder(
			final Random random,
			final JointPlan plan,
			final JointTimeModeChooserConfigGroup config,
			final ScoringFunctionFactory scoringFunctionFactory,
			final TripRouterFactory tripRouterFactory) {
		this( random , plan , config , scoringFunctionFactory , tripRouterFactory , null );
	}

	public JointTimeModeChooserConfigBuilder(
			final Random random,
			final JointPlan plan,
			final JointTimeModeChooserConfigGroup config,
			final ScoringFunctionFactory scoringFunctionFactory,
			final TripRouterFactory tripRouterFactory,
			final String analysisOutputDir) {
		this.random = random;
		this.plan = plan;
		this.config = config;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.outputDir = analysisOutputDir;
	}

	public void buildConfiguration(
			final boolean penalizeUnsynchro,
			final Solution initialSolution,
			final TabuSearchConfiguration configuration) {
		int cliqueSize = plan.getIndividualPlans().size();

		// different parameters depending on whether we optimise
		// with or without synchro, as mode is not optimised when
		// synchro (thus structure of the tabu list is different),
		// and we mainly want hill-climbing.
		int nTabu = penalizeUnsynchro ?
			1 :
			initialSolution.getRepresentation().size();
		if (nTabu < 1) nTabu = 1;
		int improvementDelay = penalizeUnsynchro ? 3 : nTabu;

		FitnessFunction fitness =
			new JointTimeModeChooserFitness(
					config.getNegativeDurationPenalty(),
					penalizeUnsynchro ? config.getUnsynchronizedPenalty() : 0,
					scoringFunctionFactory );
		configuration.setFitnessFunction( fitness );

		configuration.setEvolutionMonitor(
				new ImprovementDelayMonitor(
					improvementDelay,
					N_ITER ));

		CompositeMoveGenerator generator = new CompositeMoveGenerator();
		generator.add( new FixedStepsIntegerMovesGenerator(
					initialSolution,
					penalizeUnsynchro ?
						RESTRICTED_STEPS :
						config.getDurationSteps(),
					false));
		if (!penalizeUnsynchro) {
			// if synchro, just durations are optimized
			generator.add( new SubtourAndParentsModeMoveGenerator(
						random,
						initialSolution,
						config.getModes(),
						1 / 3d) );
		}
		configuration.setMoveGenerator( generator );

		CompositeTabuChecker tabuChecker = new CompositeTabuChecker();
		tabuChecker.add( new DirectionTabuList( nTabu ) );
		tabuChecker.add( new JointInvalidValueChecker() );
		tabuChecker.add( new ModeChainTabuList( nTabu ) );
		tabuChecker.add( new NullMoveChecker() );
		configuration.setTabuChecker( tabuChecker );

		if (outputDir != null) {
			configuration.addListener(
					new EvolutionPlotter(
						"score evolution, clique "+plan.getPerson().getId()+", "+cliqueSize+" members",
						outputDir+"/"+plan.getPerson().getId()+
						(penalizeUnsynchro ? "-synchro-" : "-preSynchro-") +
						"fitness.png" ) );
		}
	}
}
