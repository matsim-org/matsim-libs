/* *********************************************************************** *
 * project: org.matsim.*
 * EvolutionMonitor.java
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

/**
 * Interface for objects responsible of deciding whether to continue the search
 * or not.
 * @author thibautd
 */
public interface EvolutionMonitor<T> extends Listener<T> {
	/**
	 * Method called after each iteration
	 * @param iteration the number of the iteration which just finishes (starts at 0)
	 * @param currentBest the current best solution
	 * @param currentBestScore the score of the best solution
	 * @return true to continue to next iteration, false to stop the search
	 */
	public boolean continueIterations(
			final int iteration,
			final Solution<? extends T> currentBest,
			final double currentBestScore);
}

