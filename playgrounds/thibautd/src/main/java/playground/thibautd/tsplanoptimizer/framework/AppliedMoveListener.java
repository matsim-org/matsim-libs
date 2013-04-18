/* *********************************************************************** *
 * project: org.matsim.*
 * AppliedMoveListener.java
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
 * To be implemented by classes needing to be notified of the applied moves.
 * The most staightforard one is the tabu checker, but it may be used for analysis
 * as well.
 *
 * @author thibautd
 */
public interface AppliedMoveListener<T> extends Listener<T> {
	/**
	 * Notifies the application of a move.
	 * @param currentSolution the solution before application of the move
	 * @param toApply the move to apply in this iteration. It may be null, for example
	 * if the whole neighborhood is tabu.
	 * @param resultingFitness the fitness resulting from the move
	 */
	public void notifyMove( Solution<? extends T> currentSolution , Move toApply , double resultingFitness );
}

