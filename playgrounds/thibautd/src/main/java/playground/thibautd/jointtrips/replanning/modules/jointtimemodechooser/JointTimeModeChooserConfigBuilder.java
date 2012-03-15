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
package playground.thibautd.jointtrips.replanning.modules.jointtimemodechooser;

import java.util.Arrays;
import java.util.List;

import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.router.TripRouterFactory;
import playground.thibautd.tsplanoptimizer.framework.CompositeMoveGenerator;
import playground.thibautd.tsplanoptimizer.framework.CompositeTabuChecker;
import playground.thibautd.tsplanoptimizer.framework.ConfigurationBuilder;
import playground.thibautd.tsplanoptimizer.framework.EvolutionPlotter;
import playground.thibautd.tsplanoptimizer.framework.FitnessFunction;
import playground.thibautd.tsplanoptimizer.framework.ImprovementDelayMonitor;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.SolutionTabuList;
import playground.thibautd.tsplanoptimizer.framework.TabuSearchConfiguration;
import playground.thibautd.tsplanoptimizer.timemodechooser.AllPossibleModesMovesGenerator;
import playground.thibautd.tsplanoptimizer.timemodechooser.DirectionTabuList;
import playground.thibautd.tsplanoptimizer.timemodechooser.FixedStepsIntegerMovesGenerator;
import playground.thibautd.tsplanoptimizer.timemodechooser.ModeMovesTabuList;

/**
 * @author thibautd
 */
public class JointTimeModeChooserConfigBuilder implements ConfigurationBuilder {
	private static final List<Integer> STEPS = Arrays.asList( new Integer[]{ 1 * 60 , 5 * 60 , 25 * 60 , 125 * 60 , 625 * 60 });
	private static final List<String> POSSIBLE_MODES = Arrays.asList( new String[]{ "car" , "pt" , "walk" , "bike" } );
	private static final int N_ITER = 1000;

	private final JointPlan plan;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final TripRouterFactory tripRouterFactory;
	private final String outputDir;

	/**
	 * Creates a builder for optimisation without debuging output
	 * @param plan
	 * @param scoringFunctionFactory
	 * @param tripRouterFactory
	 */
	public JointTimeModeChooserConfigBuilder(
			final JointPlan plan,
			final ScoringFunctionFactory scoringFunctionFactory,
			final TripRouterFactory tripRouterFactory) {
		this( plan , scoringFunctionFactory , tripRouterFactory , null );
	}

	public JointTimeModeChooserConfigBuilder(
			final JointPlan plan,
			final ScoringFunctionFactory scoringFunctionFactory,
			final TripRouterFactory tripRouterFactory,
			final String analysisOutputDir) {
		this.plan = plan;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.tripRouterFactory = tripRouterFactory;
		this.outputDir = analysisOutputDir;
	}

	@Override
	public void buildConfiguration(final TabuSearchConfiguration configuration) {
		JointTimeModeChooserSolution initialSolution =
			new JointTimeModeChooserSolution(
					plan,
					tripRouterFactory.createTripRouter() );
		configuration.setInitialSolution( initialSolution );

		int nTabu = initialSolution.getRepresentation().size();
		if (nTabu < 1) nTabu = 1;
		int improvementDelay = 2 * nTabu;

		FitnessFunction fitness = new JointTimeModeChooserFitness( scoringFunctionFactory );
		configuration.setFitnessFunction( fitness );

		configuration.setEvolutionMonitor(
				new ImprovementDelayMonitor(
					improvementDelay,
					N_ITER ));

		CompositeMoveGenerator generator = new CompositeMoveGenerator();
		generator.add( new FixedStepsIntegerMovesGenerator(
					initialSolution,
					STEPS,
					false));
		generator.add( new AllPossibleModesMovesGenerator(
					initialSolution,
					POSSIBLE_MODES) );
		configuration.setMoveGenerator( generator );

		CompositeTabuChecker tabuChecker = new CompositeTabuChecker();
		tabuChecker.add( new DirectionTabuList( nTabu ) );
		tabuChecker.add( new JointInvalidValueChecker() );
		tabuChecker.add( new ModeMovesTabuList( nTabu ) );
		// tabuChecker.add( new SolutionTabuList( N_ITER ) );
		// tabuChecker.add( new PlanTabuList( initialSolution ) );
		configuration.setTabuChecker( tabuChecker );

		if (outputDir != null) {
			configuration.addListener(
					new EvolutionPlotter(
						"score evolution, clique "+plan.getPerson().getId(),
						outputDir+"/"+plan.getPerson().getId()+"-fitness.png" ) );
		}
	}
}
