/* *********************************************************************** *
 * project: org.matsim.*
 * DirectionTabuList.java
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

import java.util.LinkedList;

import playground.thibautd.tsplanoptimizer.framework.Move;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.TabuChecker;

/**
 * For {@link IntegerValueChanger} moves, remembers the last direction
 * applied and forbids its opposite ("directed" search).
 *
 * @author thibautd
 */
public class DirectionTabuList implements TabuChecker {
	private final LinkedList<Direction> tabuList = new LinkedList<Direction>();
	private final int capacity;

	public DirectionTabuList(final int capacity) {
		this.capacity = capacity;
	}

	@Override
	public void notifyMove(
			final Solution solution,
			final Move move,
			final double newFitness) {
		if (move instanceof IntegerValueChanger) {
			tabuList.addLast( move != null ? new Direction( move ) : null );
		}

		while (tabuList.size() > capacity) {
			tabuList.removeFirst();
		}
	}

	@Override
	public boolean isTabu(
			final Solution solution,
			final Move move) {
		if (move instanceof IntegerValueChanger) {
			return tabuList.contains( new Direction( move ) );
		}

		return false;
	}

	private static class Direction {
		private final int index;
		private final int amount;

		public Direction(final Move move) {
			this( (IntegerValueChanger) move );
		}

		public Direction(final IntegerValueChanger move) {
			index = move.getIndex();
			amount = move.getAmount();
		}

		public int hashCode() {
			return index + 1000 * amount;
		}

		public boolean equals(final Object other) {
			if (other instanceof Direction) {
				Direction direction = (Direction) other;

				if ((direction.index == index) &&
						(direction.amount * amount > 0)) {
					return true;
				}
			}

			return false;
		}
	}
}

