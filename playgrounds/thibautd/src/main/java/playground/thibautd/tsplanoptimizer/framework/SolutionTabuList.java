/* *********************************************************************** *
 * project: org.matsim.*
 * SolutionTabuList.java
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
 * The most basic tabu list, which forbids to come back to an already examined
 * solution.
 *
 * @author thibautd
 */
public class SolutionTabuList implements TabuChecker<Object> {
	private final LinkedList<Solution<?>> tabuList = new LinkedList<Solution<?>>();
	private final int capacity;

	/**
	 * @param capacity the maximum number of elements in the list (when full,
	 * the older elements will be forgotten)
	 */
	public SolutionTabuList(final int capacity) {
		this.capacity = capacity > 0 ? capacity : Integer.MAX_VALUE;
	}

	@Override
	public void notifyMove(
			final Solution<?> solution,
			final Move move,
			final double newFitness) {
		while (tabuList.size() >= capacity) {
			// remove the older
			tabuList.removeFirst();
		}

		tabuList.add( move != null ? move.apply( solution ) : solution );
	}

	@Override
	public boolean isTabu(
			final Solution<?> solution,
			final Move move) {
		return tabuList.contains( move.apply( solution ) );
	}
}

