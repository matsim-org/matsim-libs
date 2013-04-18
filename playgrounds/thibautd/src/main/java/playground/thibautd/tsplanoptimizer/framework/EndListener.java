/* *********************************************************************** *
 * project: org.matsim.*
 * EndListener.java
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
 * Inteface to implement to be notified of the state at the end of the optimisation.
 * @author thibautd
 */
public interface EndListener<T> extends Listener<T> {
	/**
	 * @param bestSolution the best solution found in the search
	 * @param bestScore the score of the solution
	 * @param nIterations the number of iterations after which the search was stopped
	 */
	public void notifyEnd(
			final Solution<? extends T> bestSolution,
			final double bestScore,
			final int nIterations);
}

