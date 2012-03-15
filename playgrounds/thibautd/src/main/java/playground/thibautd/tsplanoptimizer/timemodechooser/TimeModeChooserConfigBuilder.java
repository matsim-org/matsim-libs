/* *********************************************************************** *
 * project: org.matsim.*
 * TimeModeChooserConfigBuilder.java
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
package playground.thibautd.tsplanoptimizer.timemodechooser;

import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.thibautd.router.TripRouterFactory;
import playground.thibautd.tsplanoptimizer.framework.CompositeMoveGenerator;
import playground.thibautd.tsplanoptimizer.framework.CompositeTabuChecker;
import playground.thibautd.tsplanoptimizer.framework.ConfigurationBuilder;
import playground.thibautd.tsplanoptimizer.framework.EvolutionPlotter;
import playground.thibautd.tsplanoptimizer.framework.FitnessFunction;
import playground.thibautd.tsplanoptimizer.framework.ImprovementDelayMonitor;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.TabuSearchConfiguration;

/**
 * @author thibautd
 */
public class TimeModeChooserConfigBuilder implements ConfigurationBuilder {
	private static final List<Integer> STEPS = Arrays.asList( new Integer[]{ 1 * 60 , 5 * 60 , 30 * 60, 2 * 3600 });
	private static final List<String> POSSIBLE_MODES = Arrays.asList( new String[]{ "car" , "pt" , "walk" , "bike" } );
	private static final int N_ITER = 1000;
	// TODO: function of the number of activities
	private static final int N_TABU_DIRECTION = 10;
	private static final int N_TABU_MODE = N_TABU_DIRECTION;
	// TODO: function of the length of the tabu list
	private static final int IMPROVEMENT_DELAY = 100;

	private final Plan plan;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final TripRouterFactory tripRouterFactory;
	private final String outputDir;

	/**
	 * Creates a builder for optimisation without debuging output
	 * @param plan
	 * @param scoringFunctionFactory
	 * @param tripRouterFactory
	 */
	public TimeModeChooserConfigBuilder(
			final Plan plan,
			final ScoringFunctionFactory scoringFunctionFactory,
			final TripRouterFactory tripRouterFactory) {
		this( plan , scoringFunctionFactory , tripRouterFactory , null );
	}

	public TimeModeChooserConfigBuilder(
			final Plan plan,
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
		Solution initialSolution =
			new TimeModeChooserSolution(
					plan,
					tripRouterFactory.createTripRouter() );
		configuration.setInitialSolution( initialSolution );

		FitnessFunction fitness = new BasicFitness( scoringFunctionFactory );
		configuration.setFitnessFunction( fitness );

		configuration.setEvolutionMonitor(
				new ImprovementDelayMonitor(
					IMPROVEMENT_DELAY,
					N_ITER ));

		CompositeMoveGenerator generator = new CompositeMoveGenerator();
		generator.add( new FixedStepsIntegerMovesGenerator(
					initialSolution,
					STEPS,
					true));
					//new ModeOptimizingMoveConfigBuilder(
					//	fitness,
					//	POSSIBLE_MODES)) );
		generator.add( new AllPossibleModesMovesGenerator(
					initialSolution,
					POSSIBLE_MODES) );
		configuration.setMoveGenerator( generator );

		CompositeTabuChecker tabuChecker = new CompositeTabuChecker();
		tabuChecker.add( new DirectionTabuList( N_TABU_DIRECTION ) );
		tabuChecker.add( new InvalidSolutionsTabuList() );
		tabuChecker.add( new ModeMovesTabuList( N_TABU_MODE ) );
		configuration.setTabuChecker( tabuChecker );

		if (outputDir != null) {
			configuration.addListener(
					new EvolutionPlotter(
						"score evolution, agent "+plan.getPerson().getId(),
						outputDir+"/"+plan.getPerson().getId()+"-fitness.png" ) );
		}
	}
}

