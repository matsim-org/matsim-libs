/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripsSelectorConfigBuilder.java
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
package playground.thibautd.jointtrips.replanning.modules.jointtripsselector;

import org.apache.log4j.Logger;

import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.tsplanoptimizer.framework.AppliedMoveListener;
import playground.thibautd.tsplanoptimizer.framework.ConfigurationBuilder;
import playground.thibautd.tsplanoptimizer.framework.ImprovementDelayMonitor;
import playground.thibautd.tsplanoptimizer.framework.IterationNumberMonitor;
import playground.thibautd.tsplanoptimizer.framework.Move;
import playground.thibautd.tsplanoptimizer.framework.MoveTabuList;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.StartListener;
import playground.thibautd.tsplanoptimizer.framework.TabuSearchConfiguration;

/**
 * @author thibautd
 */
public class JointTripsSelectorConfigBuilder implements ConfigurationBuilder {
	private static final Logger log =
		Logger.getLogger(JointTripsSelectorConfigBuilder.class);

	private final static boolean SEQUENTIAL = true;
	private final static boolean DEBUG_LOG_LEVEL = false;
	private final static int N_ITER = 5;
	private final static int N_TABU = N_ITER;

	private final JointPlan plan;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final PlanAlgorithm optimisationRoutine;

	public JointTripsSelectorConfigBuilder(
			final JointPlan plan,
			final ScoringFunctionFactory scoringFunctionFactory,
			final PlanAlgorithm optimisationRoutine) {
		this.plan = plan;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.optimisationRoutine = optimisationRoutine;
	}

	@Override
	public void buildConfiguration(final TabuSearchConfiguration configuration) {
		Solution initialSolution =
			new JointTripsSelectorSolution(
					plan,
					optimisationRoutine);
		configuration.setInitialSolution( initialSolution );
		configuration.setFitnessFunction(
				new BasicJointPlanFitnessFunction( scoringFunctionFactory ));

		if (SEQUENTIAL) {
			configuration.setEvolutionMonitor(
					new IterationNumberMonitor(
						initialSolution.getRepresentation().size() ));
			configuration.setMoveGenerator(
					new JointTripsSelectorSequentialMoveGenerator(
						MatsimRandom.getLocalInstance(),
						initialSolution.getRepresentation().size() ) );
		}
		else {
			configuration.setEvolutionMonitor(
					new ImprovementDelayMonitor(
						1,
						N_ITER ));
			configuration.setMoveGenerator(
					new JointTripsSelectorMoveGenerator(
						initialSolution.getRepresentation().size() ) );
		}

		configuration.setTabuChecker( new MoveTabuList( N_TABU ) );

		if (DEBUG_LOG_LEVEL) {
			configuration.addListener( new Printer() );
		}
	}

	class Printer implements AppliedMoveListener, StartListener {
		@Override
		public void notifyMove(
				final Solution currentSolution,
				final Move toApply,
				final double resultingFitness) {
			print( toApply != null ? toApply.apply( currentSolution ) : null , resultingFitness );
		}

		@Override
		public void notifyStart(
				final Solution startSolution,
				final double startScore) {
			print( startSolution , startScore );
		}

		private void print( final Solution sol , final double score ) {
			log.debug( "Clique "+plan.getClique().getId()+" : "+ (sol != null ? sol.getRepresentation() : null) +" : "+score );
		}
	}
}
