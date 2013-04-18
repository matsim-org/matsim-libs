/* *********************************************************************** *
 * project: org.matsim.*
 * MoveTabuList.java
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

import java.util.LinkedList;

/**
 * A tabu list which forbids reverse moves of the previouly applied moves.
 * Note that it works only for moves for which the {@link Move#getReverseMove()}
 * method is implemented.
 *
 * @author thibautd
 */
public class MoveTabuList<T> implements TabuChecker<T> {
	private final LinkedList<Move> tabuMoves = new LinkedList<Move>();
	private final int capacity;

	/**
	 * @param capacity the maximum number of elements in the list (when full,
	 * the older elements will be forgotten)
	 */
	public MoveTabuList(final int capacity) {
		this.capacity = capacity > 0 ? capacity : Integer.MAX_VALUE;
	}

	@Override
	public void notifyMove(
			final Solution<? extends T> solution,
			final Move move,
			final double newFitness) {
		while (tabuMoves.size() >= capacity) {
			// remove the older
			tabuMoves.removeFirst();
		}

		// even if no move, add something, to enforce forgetting
		// old solutions (tabu list length must correspond to the
		// number of iterations a move is reminded)
		tabuMoves.add( move != null ? move.getReverseMove() : null );
	}

	@Override
	public boolean isTabu(
			final Solution<? extends T> solution,
			final Move move) {
		return tabuMoves.contains( move );
	}
}

