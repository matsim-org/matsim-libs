/* *********************************************************************** *
 * project: org.matsim.*
 * TabuSearchRunner.java
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
package playground.thibautd.tsplanoptimizer.framework;

import java.util.Collection;
import java.util.List;

/**
 * Class responsible for running the tabu search defined by tuning
 * a {@link TabuSearchConfiguration}.
 * To build a best-response module based on tabu search,
 * the plan algorithm mainly has to tune the configuration
 * and launch the run method.
 *
 * @author thibautd
 */
public final class TabuSearchRunner {
	private TabuSearchRunner() {}

	public static <T> Solution<T> runTabuSearch(
			final TabuSearchConfiguration<T> configuration,
			final Solution<T> initialSolution ) {
		// get the elements
		// -----------------------------------------------------------
		EvolutionMonitor<T> monitor = configuration.getEvolutionMonitor();
		MoveGenerator moveGenerator = configuration.getMoveGenerator();
		TabuChecker<T> tabu = configuration.getTabuChecker();
		FitnessFunction<T> fitness = configuration.getFitnessFunction();
		List<AppliedMoveListener<T>> listeners = configuration.getAppliedMoveListeners();

		Solution<T> currentSolution = initialSolution;

		Solution<T> currentBestSolution = currentSolution.createClone();
		double currentBestScore = fitness.computeFitnessValue( currentBestSolution );

		for (StartListener<T> listener : configuration.getStartListeners()) {
			listener.notifyStart( currentSolution , currentBestScore );
		}

		// first iteration: iteration 0.
		int iteration = -1;
		do {
			iteration++;
			double iterationBestScore = Double.NEGATIVE_INFINITY;
			Move iterationBestMove = null;
			Solution<T> iterationBestSolution = null;
			Collection<Move> moves = moveGenerator.generateMoves();
			
			// for each move, if not tabu, generate the solution, and cache the
			// best solution of the neighborhood
			// -----------------------------------------------------------------
			for (Move move : moves) {
				if (!tabu.isTabu( currentSolution , move )) {
					Solution<T> newSolution = move.apply( currentSolution );
					double newScore = fitness.computeFitnessValue( newSolution );

					if (newScore > iterationBestScore) {
						iterationBestMove = move;
						iterationBestSolution = newSolution;
						iterationBestScore = newScore;
					}
					else if (Double.isNaN( newScore ) || Double.isInfinite( newScore )) {
						throw new RuntimeException( "got invalid score: "+newScore );
					}
				}
			}

			// now, we know the move to apply: notify it to all registered
			// listeners
			// -----------------------------------------------------------------
			for (AppliedMoveListener<T> listener : listeners) {
				// this includes the tabu checker.
				listener.notifyMove( currentSolution , iterationBestMove , iterationBestScore );
			}

			// update the current and current best solutions.
			// -----------------------------------------------------------------
			if (iterationBestSolution != null) {
				// if the neighborhood was empty, stay at the same solution,
				// hoping that changes in the tabu status will allow some moves.
				currentSolution = iterationBestSolution;
			}
			if (iterationBestScore > currentBestScore) {
				currentBestSolution = currentSolution;
				currentBestScore = iterationBestScore;
			}
		}
		while ( monitor.continueIterations( iteration , currentBestSolution , currentBestScore ) );

		for (EndListener<T> listener : configuration.getEndListeners()) {
			listener.notifyEnd( currentSolution , currentBestScore , iteration );
		}

		return currentBestSolution;
	}
}

