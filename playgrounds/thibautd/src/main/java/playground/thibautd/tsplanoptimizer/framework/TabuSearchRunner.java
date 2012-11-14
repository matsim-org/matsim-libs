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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.NetworkRoute;

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
	private static final Logger log =
		Logger.getLogger(TabuSearchRunner.class);

	private TabuSearchRunner() {}

	public static Solution runTabuSearch(
			final TabuSearchConfiguration configuration,
			final Solution initialSolution ) {
		// get the elements
		// -----------------------------------------------------------
		EvolutionMonitor monitor = configuration.getEvolutionMonitor();
		MoveGenerator moveGenerator = configuration.getMoveGenerator();
		TabuChecker tabu = configuration.getTabuChecker();
		FitnessFunction fitness = configuration.getFitnessFunction();
		List<AppliedMoveListener> listeners = configuration.getAppliedMoveListeners();

		Solution currentSolution = initialSolution;

		Solution currentBestSolution = currentSolution.createClone();
		double currentBestScore = fitness.computeFitnessValue( currentBestSolution );

		for (StartListener listener : configuration.getStartListeners()) {
			listener.notifyStart( currentSolution , currentBestScore );
		}

		// first iteration: iteration 0.
		int iteration = -1;
		do {
			iteration++;
			double iterationBestScore = Double.NEGATIVE_INFINITY;
			Move iterationBestMove = null;
			Solution iterationBestSolution = null;
			Collection<Move> moves = moveGenerator.generateMoves();
			
			// for each move, if not tabu, generate the solution, and cache the
			// best solution of the neighborhood
			// -----------------------------------------------------------------
			for (Move move : moves) {
				if (!tabu.isTabu( currentSolution , move )) {
					Solution newSolution = move.apply( currentSolution );
					double newScore = fitness.computeFitnessValue( newSolution );

					if (newScore > iterationBestScore) {
						iterationBestMove = move;
						iterationBestSolution = newSolution;
						iterationBestScore = newScore;
					}
					else if (Double.isNaN( newScore ) || Double.isInfinite( newScore )) {
						logInvalidSolution( newSolution );
						throw new RuntimeException( "got invalid score: "+newScore );
					}
				}
			}

			// now, we know the move to apply: notify it to all registered
			// listeners
			// -----------------------------------------------------------------
			for (AppliedMoveListener listener : listeners) {
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

		for (EndListener listener : configuration.getEndListeners()) {
			listener.notifyEnd( currentSolution , currentBestScore , iteration );
		}

		return currentBestSolution;
	}

	/**
	 * Helps debuging: logs all plan elements of a plan getting an incorrect score.
	 */
	private static void logInvalidSolution(final Solution newSolution) {
		log.error( "INVALID SOLUTION!" );
		int i=0;
		for (PlanElement pe : newSolution.getRepresentedPlan().getPlanElements()) {
			log.error( (++i)+": "+pe );
			if (pe instanceof Leg) {
				Route route = ((Leg) pe).getRoute();
				if (route instanceof GenericRoute) {
					log.error( "     "+((GenericRoute) route).getRouteType()+" from "+route.getStartLinkId()+" to "+route.getEndLinkId()+": "+((GenericRoute) route).getRouteDescription() );
				}
				else if (route instanceof NetworkRoute) {
					log.error( "     "+route.getClass().getSimpleName()+" from "+route.getStartLinkId()+" to "+route.getEndLinkId()+": "+((NetworkRoute) route).getLinkIds() );
				}
			}
		}
	}
}

