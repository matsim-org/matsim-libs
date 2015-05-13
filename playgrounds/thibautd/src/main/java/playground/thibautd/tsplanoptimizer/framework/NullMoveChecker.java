/* *********************************************************************** *
 * project: org.matsim.*
 * NullMoveChecker.java
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

import java.util.Iterator;

/**
 * Forbids the moves which do not change the solution.
 *
 * @author thibautd
 */
public class NullMoveChecker<T> implements TabuChecker<T> {

	@Override
	public void notifyMove(
			final Solution<? extends T> currentSolution,
			final Move toApply,
			final double resultingFitness) {
		// do nothing
	}

	@Override
	public boolean isTabu(
			final Solution<? extends T> solution,
			final Move move) {
		final Solution<?> newSolution = move.apply( solution );

		final Iterator<? extends Value> oldIt = solution.getGenotype().iterator();
		final Iterator<? extends Value> newIt = newSolution.getGenotype().iterator();

		while (oldIt.hasNext()) {
			if (!oldIt.next().getValue().equals( newIt.next().getValue() )) {
				// a different value was found
				return false;
			}
		}

		if (newIt.hasNext()) {
			throw new RuntimeException( "unexpected new solution length" );
		}

		// all values were identical
		return true;
	}
}

